#!/usr/bin/env python3
"""Quick script to generate servers.json from VPN Gate data"""

import json
import base64
import re
from datetime import datetime

GOOGLE_COUNTRIES = ['JP', 'US', 'KR', 'TW', 'SG', 'HK', 'DE', 'GB', 'FR', 'NL', 'AU', 'CA']

def main():
    # Read CSV
    with open(r"D:\project\AutoGuardVPN\vpngate_data.csv", 'r', encoding='utf-8', errors='ignore') as f:
        lines = f.readlines()
    
    # Find header
    header_idx = -1
    for i, line in enumerate(lines):
        if '#HostName' in line:
            header_idx = i
            break
    
    if header_idx == -1:
        print("Header not found")
        return
    
    servers = []
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
            
            # Only Google-accessible countries with OpenVPN config
            if country_short not in GOOGLE_COUNTRIES:
                continue
            if not config_base64 or score < 100000:
                continue
            
            # Parse config for port
            try:
                config = base64.b64decode(config_base64).decode('utf-8', errors='ignore')
                remote_match = re.search(r'remote\s+(\S+)\s+(\d+)', config)
                port = int(remote_match.group(2)) if remote_match else 443
            except:
                port = 443
                config = ""
            
            servers.append({
                "id": str(abs(hash(ip))),
                "name": hostname,
                "country": country_short,
                "city": country_long,
                "endpoint": ip,
                "port": port,
                "protocol": "OpenVPN",
                "config": config,
                "username": "vpn",
                "password": "vpn",
                "psk": "vpn",
                "pingLatency": ping,
                "vpnSessions": vpn_sessions,
                "throughput": round(speed / 1024 / 1024, 2),
                "score": score
            })
        except Exception as e:
            continue
    
    # Sort by score and take top 20
    servers.sort(key=lambda x: x.get('score', 0), reverse=True)
    
    # Remove score field and take top 20
    final_servers = []
    for s in servers[:20]:
        del s['score']
        final_servers.append(s)
    
    output = {
        "servers": final_servers,
        "version": "2.0",
        "lastUpdated": datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    }
    
    with open(r"D:\project\AutoGuardVPN\app\src\main\assets\servers.json", 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    
    print(f"Generated {len(final_servers)} servers")
    for i, s in enumerate(final_servers[:5], 1):
        print(f"  {i}. {s['endpoint']}:{s['port']} - {s['city']} ({s['country']})")

if __name__ == "__main__":
    main()
