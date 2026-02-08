#!/usr/bin/env python3
"""
VPN Gate Server List Updater
Downloads and parses VPN Gate API data, tests connectivity, and generates servers.json
Uses OpenVPN configuration from VPN Gate servers
"""

import urllib.request
import json
import base64
import socket
import re
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

VPNGATE_API_URL = "https://www.vpngate.net/api/iphone/"

# Countries that can access Google
GOOGLE_ACCESSIBLE_COUNTRIES = ['JP', 'US', 'KR', 'TW', 'SG', 'HK', 'DE', 'GB', 'FR', 'NL', 'AU', 'CA', 'SE', 'CH', 'NO', 'FI', 'DK', 'BE', 'AT', 'IT', 'ES', 'PT', 'IE', 'NZ', 'PL', 'CZ', 'RO', 'HU', 'TH', 'MY', 'PH', 'VN', 'IN', 'ID']

def download_vpngate_data():
    """Download VPN Gate server list"""
    print("Downloading VPN Gate server list...")
    try:
        req = urllib.request.Request(VPNGATE_API_URL, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        with urllib.request.urlopen(req, timeout=30) as response:
            data = response.read().decode('utf-8', errors='ignore')
            print(f"Downloaded {len(data)} bytes")
            return data
    except Exception as e:
        print(f"Download failed: {e}")
        return None

def parse_vpngate_csv(csv_data):
    """Parse VPN Gate CSV data"""
    servers = []
    lines = csv_data.strip().split('\n')
    
    # Find header line
    header_idx = -1
    for i, line in enumerate(lines):
        if line.startswith('#HostName') or line.startswith('*HostName'):
            header_idx = i
            break
    
    if header_idx == -1:
        print("Could not find header line")
        return servers
    
    # Parse data lines
    for line in lines[header_idx + 1:]:
        if line.startswith('*') or not line.strip():
            continue
        
        parts = line.split(',')
        if len(parts) < 15:
            continue
        
        try:
            hostname = parts[0]
            ip = parts[1]
            score = int(parts[2]) if parts[2] else 0
            ping = int(parts[3]) if parts[3] else 0
            speed = int(parts[4]) if parts[4] else 0
            country_long = parts[5]
            country_short = parts[6]
            vpn_sessions = int(parts[7]) if parts[7] else 0
            config_base64 = parts[14] if len(parts) > 14 else ""
            
            # Only include servers with OpenVPN config
            if not config_base64:
                continue
            
            servers.append({
                'hostname': hostname,
                'ip': ip,
                'score': score,
                'ping': ping,
                'speed': speed,
                'country_long': country_long,
                'country_short': country_short,
                'vpn_sessions': vpn_sessions,
                'config_base64': config_base64
            })
        except (ValueError, IndexError) as e:
            continue
    
    print(f"Parsed {len(servers)} servers with OpenVPN config")
    return servers

def parse_openvpn_config(config_base64):
    """Parse OpenVPN config to extract connection details"""
    try:
        config = base64.b64decode(config_base64).decode('utf-8', errors='ignore')
        
        # Extract remote server and port
        remote_match = re.search(r'remote\s+(\S+)\s+(\d+)', config)
        if remote_match:
            host = remote_match.group(1)
            port = int(remote_match.group(2))
        else:
            return None, None, None
        
        # Extract protocol (tcp/udp)
        proto_match = re.search(r'proto\s+(tcp|udp)', config)
        proto = proto_match.group(1) if proto_match else 'udp'
        
        return host, port, proto
    except Exception as e:
        return None, None, None

def test_server_connectivity(ip, port, timeout=5):
    """Test if server is reachable"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex((ip, port))
        sock.close()
        return result == 0
    except Exception:
        return False

def test_server(server):
    """Test a single server's connectivity"""
    ip = server['ip']
    config_base64 = server['config_base64']
    
    # Parse config to get actual connection port
    host, port, proto = parse_openvpn_config(config_base64)
    if not port:
        port = 443  # Default OpenVPN port
    
    # Test connectivity
    if test_server_connectivity(ip, port):
        return server, port, True
    
    # Try alternative ports
    for alt_port in [443, 1194, 992, 995]:
        if alt_port != port and test_server_connectivity(ip, alt_port):
            return server, alt_port, True
    
    return server, port, False

def convert_to_vpnserver(server, port):
    """Convert parsed server data to VpnServer format"""
    return {
        "id": str(abs(hash(server['ip']))),
        "name": server['hostname'],
        "country": server['country_short'],
        "city": server['country_long'],
        "endpoint": server['ip'],
        "port": port,
        "protocol": "OpenVPN",
        "config": base64.b64decode(server['config_base64']).decode('utf-8', errors='ignore'),
        "username": "vpn",
        "password": "vpn",
        "psk": "vpn",
        "pingLatency": server['ping'],
        "vpnSessions": server['vpn_sessions'],
        "throughput": round(server['speed'] / 1024 / 1024, 2)
    }

def main():
    # Download data
    csv_data = download_vpngate_data()
    if not csv_data:
        print("Failed to download VPN Gate data")
        return
    
    # Parse CSV
    servers = parse_vpngate_csv(csv_data)
    if not servers:
        print("No servers found in data")
        return
    
    # Filter by Google-accessible countries and high score
    filtered = [s for s in servers 
                if s['country_short'] in GOOGLE_ACCESSIBLE_COUNTRIES 
                and s['score'] >= 100000]
    
    print(f"Filtered to {len(filtered)} servers in Google-accessible countries with score >= 100000")
    
    # Sort by score (highest first)
    filtered.sort(key=lambda x: x['score'], reverse=True)
    
    # Take top 50 for testing
    candidates = filtered[:50]
    
    # Test connectivity in parallel
    print(f"Testing connectivity for {len(candidates)} servers...")
    working_servers = []
    
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = {executor.submit(test_server, s): s for s in candidates}
        
        for future in as_completed(futures):
            server, port, is_working = future.result()
            if is_working:
                working_servers.append((server, port))
                print(f"  ✓ {server['ip']}:{port} ({server['country_short']}) - Score: {server['score']}")
            else:
                print(f"  ✗ {server['ip']} ({server['country_short']}) - Not reachable")
    
    print(f"\nFound {len(working_servers)} working servers")
    
    if not working_servers:
        print("No working servers found!")
        return
    
    # Sort working servers by score
    working_servers.sort(key=lambda x: x[0]['score'], reverse=True)
    
    # Take top 20 working servers
    final_servers = working_servers[:20]
    
    # Convert to VpnServer format
    vpn_servers = [convert_to_vpnserver(s, p) for s, p in final_servers]
    
    # Create output structure
    output = {
        "servers": vpn_servers,
        "version": "2.0",
        "lastUpdated": datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    }
    
    # Write to servers.json
    output_path = r"D:\project\AutoGuardVPN\app\src\main\assets\servers.json"
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    
    print(f"\nSaved {len(vpn_servers)} servers to {output_path}")
    
    # Print summary
    print("\n=== Server Summary ===")
    for i, server in enumerate(vpn_servers[:10], 1):
        print(f"{i}. {server['endpoint']}:{server['port']} - {server['city']} ({server['country']}) - {server['throughput']} Mbps")

if __name__ == "__main__":
    main()
