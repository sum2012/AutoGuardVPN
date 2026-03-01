# AutoGuard VPN

A modern Android VPN application that automatically fetches node configurations from remote servers such as vpngate.net.

## Create history
The program is created by MiniMax agent. Compilation fix by Google Gemini 1.5 Pro in Android Studio.

## Features

- **VPN Gate Integration**: Supports automatic retrieval of free VPN server lists from [vpngate.net](https://www.vpngate.net/)
- **Automatic Server Sync**: Automatically fetches available server lists from the API
- **Multi-Protocol Support**: Supports various connection protocols including OpenVPN (TCP/UDP), SSL-VPN, etc.
- **Server Rating System**: Recommends high-quality servers based on the VPN Gate rating system
- **Modern UI**: Material 3 design built with Jetpack Compose
- **Server Grouping**: Displays servers grouped by country and city
- **Latency Display**: Shows connection latency for each server
- **Connection Status**: Real-time display of VPN connection status
- **Settings Options**: Supports Kill Switch, etc.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3 (with Material Icons Extended)
- **Image Loading**: Coil
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp + Gson
- **Data Storage**: DataStore Preferences
- **Concurrency**: Kotlin Coroutines + Flow

## VPN Gate Support

### Data Sources

The application prioritizes the following data sources for server lists:

1. **VPN Gate (vpngate.net)** - Default data source
   - API: `http://www.vpngate.net/api/iphone/`
   - Format: CSV
   - Features: Provides a large number of free VPN servers with real-time ratings

2. **Custom API** - Backup data source
   - Supports custom JSON format server lists
   - API address can be configured in settings

### VPN Gate CSV Format

The VPN Gate API returns server data in CSV format, containing the following fields:

| Field | Description |
|------|------|
| Country | Country/Physical Location |
| DDNS hostname | e.g., `public-vpn-152.opengw.net` |
| IP Address | Server IP Address |
| VPN sessions | Current session count |
| Uptime | Running time |
| Cumulative users | Total users |
| Throughput (Mbps) | Bandwidth speed |
| Ping (ms) | Latency |
| Logging policy | Logging policy |
| OpenVPN TCP port | OpenVPN TCP port |
| OpenVPN UDP port | OpenVPN UDP port |
| Score | Quality score |

### Connection Credentials

VPN Gate servers use common connection credentials:

- **Username**: `vpn`
- **Password**: `vpn`

### Server Filtering

The application automatically filters high-quality servers:
- Servers with a score >= 10,000
- Servers supporting the OpenVPN protocol
- Sorted by score and latency

## Project Structure

```
app/
├── src/main/
│   ├── java/com/autoguard/vpn/
│   │   ├── data/
│   │   │   ├── api/          # API Interfaces (ServerApiService, VpnGateApiService)
│   │   │   ├── model/        # Data Models (VpnServer, VpnGateServer)
│   │   │   └── repository/   # Data Repositories (ServerRepository)
│   │   ├── di/               # Dependency Injection (AppModule, Qualifiers, StringConverterFactory)
│   │   ├── service/          # VPN Service (AutoGuardVpnService)
│   │   ├── ui/
│   │   │   ├── components/   # UI Components (ConnectButton, ServerCard)
│   │   │   ├── screens/      # Screens (HomeScreen, ServerListScreen, SettingsScreen)
│   │   │   ├── theme/        # Themes (Color, Shape, Theme, Type)
│   │   │   ├── viewmodel/    # ViewModel (MainViewModel)
│   │   │   └── MainActivity.kt
│   │   └── AutoGuardApplication.kt
│   └── res/                  # Resources
└── build.gradle.kts          # Build Configuration
```

## Configuration

### Server API Format

#### VPN Gate CSV (Default)

Fetches CSV data from `http://www.vpngate.net/api/iphone/`, which the app parses automatically.

#### Custom JSON (Optional)

The app also supports custom JSON server lists:

```json
{
  "version": "1.0.0",
  "lastUpdated": "2024-01-24T00:00:00Z",
  "servers": [
    {
      "id": "us-east-1",
      "name": "US East",
      "country": "US",
      "city": "New York",
      "endpoint": "192.0.2.1:51820",
      "publicKey": "base64_encoded_public_key",
      "allowedIps": "0.0.0.0/0",
      "port": 51820,
      "pingLatency": 45
    }
  ]
}
```

### Permissions

The application requires the following permissions:

- **INTERNET**: Network access (for fetching server lists and VPN connections)
- **ACCESS_NETWORK_STATE**: Network state access
- **FOREGROUND_SERVICE**: Foreground service permission (maintaining VPN connection)
- **BIND_VPN_SERVICE**: VPN service binding permission
- **POST_NOTIFICATIONS**: Notification permission (Android 13+)

## Build Instructions

1. Ensure Android Studio Hedgehog or higher is installed
2. Open `build.gradle.kts` in the project root
3. Wait for Gradle sync to complete
4. Build and run the application

### System Requirements

- Android SDK 26+ (Android 8.0 Oreo)
- Android Gradle Plugin 8.1.0+
- Gradle 8.0+
- Kotlin 1.9.0+

## Usage

### Quick Start

1. Install and launch the application
2. The app will automatically fetch the server list from VPN Gate
3. Select a high-quality server (indicated by a green score)
4. Click the central connection button
5. Grant VPN permission on the first connection

### Selecting a Server

- Servers are grouped by country
- Displays score, latency, and bandwidth for each server
- Prefer servers with high scores and low latency
- Supports filtering by country

### Advanced Settings

- **Kill Switch**: Blocks network access when the VPN disconnects
- **Custom API**: Configure your own server list API
- **Dark Mode**: Toggle the application theme

## Notes

⚠️ **Important**:

1. This is a VPN client application and requires a VPN server to work
2. VPN Gate provides free servers, but speed and stability may vary
3. VPN Gate servers are provided by volunteers; please comply with the terms of use
4. Do not use this application for illegal purposes
5. Please comply with local laws and regulations when using a VPN

## Acknowledgments

- [VPN Gate](https://www.vpngate.net/) - Provides the free VPN server list API
- [WireGuard](https://www.wireguard.com/) - High-performance VPN protocol
- [OpenVPN](https://openvpn.net/) - Open-source VPN solution
- [Open SSTP Client](https://github.com/Kitsunemimi/Open-SSTP-Client) - Open-source SSTP VPN client for Android

## License

GNU 3 License

## Contributing

Issues and Pull Requests are welcome!
