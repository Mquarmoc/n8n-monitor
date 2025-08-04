# n8n Monitor - Android App

A minimalistic Android application for monitoring n8n workflows and executions. Built with modern Android development practices including Jetpack Compose, Material 3, and clean architecture.

## Features

### Core Functionality
- **Workflow List**: View all active n8n workflows with status indicators
- **Workflow Details**: Drill down into individual workflows to see recent executions
- **Execution Management**: View execution status, duration, and stop running executions
- **Background Monitoring**: Receive notifications for failed executions
- **Pull-to-Refresh**: Refresh data with intuitive swipe gestures

### Security & Privacy
- **Encrypted Storage**: API keys stored securely using EncryptedSharedPreferences
- **SQLCipher Database**: Local data encrypted with SQLCipher
- **Minimal Permissions**: Only requires INTERNET and POST_NOTIFICATIONS
- **No Analytics**: No user tracking or telemetry collection

### User Experience
- **Material 3 Design**: Modern UI following Material Design guidelines
- **Dark/Light Themes**: Support for both light and dark themes
- **Responsive Layout**: Works on phones and tablets
- **Offline Support**: View cached data when offline
- **Deep Links**: Navigate directly to workflows from notifications

### Settings & Configuration
- **Connection Settings**: Configure n8n base URL and API key
- **Polling Interval**: Adjustable background monitoring frequency (5-60 minutes)
- **Notification Preferences**: Enable/disable background notifications
- **Theme Toggle**: Manual control over light/dark mode
- **Biometric Authentication**: Optional biometric protection for API key access

## Architecture

### Technology Stack
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp + Moshi
- **Database**: Room with SQLCipher encryption
- **Background Work**: WorkManager
- **Settings**: DataStore
- **Navigation**: Navigation Compose

### Project Structure
```
app/src/main/java/com/example/n8nmonitor/
├── data/
│   ├── api/           # Retrofit API interface and interceptors
│   ├── database/      # Room entities, DAOs, and database
│   ├── dto/           # Data transfer objects for API responses
│   ├── repository/    # Repository layer
│   └── settings/      # DataStore for user preferences
├── di/                # Hilt dependency injection modules
├── ui/
│   ├── screen/        # Compose UI screens
│   ├── state/         # UI state classes
│   ├── theme/         # Material 3 theme configuration
│   └── viewmodel/     # ViewModels
├── worker/            # WorkManager background workers
└── N8nMonitorApplication.kt
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34
- Kotlin 1.9.20+
- n8n instance with API access

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/n8n-monitor.git
   cd n8n-monitor
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Shift+F10

### Configuration

1. **Get n8n API Key**
   - In your n8n instance, go to Settings → API
   - Create a new API key with appropriate permissions
   - Note down the API key

2. **Configure App Settings**
   - Open the app
   - Navigate to Settings
   - Enter your n8n base URL (e.g., `https://your-n8n-instance.com`)
   - Enter your API key
   - Adjust polling interval as needed
   - Enable notifications if desired

## API Requirements

The app requires n8n API v1 with the following endpoints:

- `GET /api/v1/workflows` - List workflows
- `GET /api/v1/workflows/{id}` - Get workflow details
- `GET /api/v1/executions` - List executions
- `GET /api/v1/executions/{id}` - Get execution details
- `POST /api/v1/executions/{id}/stop` - Stop execution

All requests require the `X-N8N-API-KEY` header.

## Development

### Scripts de Déploiement et Automatisation

Le projet inclut une suite complète de scripts pour automatiser le développement et le déploiement :

#### Configuration Rapide

```bash
# Configuration complète de l'environnement
bash scripts/setup-dev-env.sh

# Configuration de la signature d'application
bash scripts/setup-signing.sh

# Déploiement complet vers internal testing
bash scripts/build-and-deploy.sh
```

#### Scripts Disponibles

- **`scripts/jdk17-wrapper.sh`** : Configuration automatique de JDK 17
- **`scripts/setup-dev-env.sh`** : Configuration complète de l'environnement de développement
- **`scripts/setup-signing.sh`** : Configuration de la signature d'application
- **`scripts/security-audit.sh`** : Audit de sécurité automatisé
- **`scripts/upload-aab.sh`** : Construction et upload AAB (Linux/macOS)
- **`scripts/upload-aab.ps1`** : Version PowerShell pour Windows
- **`scripts/build-and-deploy.sh`** : Script principal orchestrant tout le processus

#### Configuration Fastlane

Le projet utilise fastlane pour automatiser les déploiements :

```bash
# Installation des dépendances
bundle install

# Tests et audit
fastlane android test
fastlane android security_audit

# Construction
fastlane android release_aab

# Déploiement
fastlane android internal    # Internal testing
fastlane android alpha       # Alpha track
fastlane android beta        # Beta track
fastlane android production  # Production

# Pipeline complet
fastlane android ci
```

#### Variables d'Environnement

Copiez `.env.example` vers `.env` et configurez :

```bash
# Google Play Console Service Account
GOOGLE_PLAY_JSON_KEY_PATH=/path/to/service-account.json
# ou
GOOGLE_PLAY_JSON_KEY_DATA={"type":"service_account",...}

# App Signing
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```

### Building for Release

#### Méthode Automatisée (Recommandée)

```bash
# Configuration initiale (une seule fois)
bash scripts/setup-signing.sh

# Déploiement vers Google Play
bash scripts/build-and-deploy.sh --track production
```

#### Méthode Manuelle

1. **Generate Release Keystore**
   ```bash
   keytool -genkey -v -keystore n8n-monitor.keystore -alias n8n-monitor -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Signing**
   - Add keystore details to `app/build.gradle.kts`
   - Set up signing configuration

3. **Build Release AAB**
   ```bash
   ./gradlew bundleRelease
   ```

### Testing

The project includes:
- **Unit Tests**: Repository and ViewModel tests
- **UI Tests**: Compose UI testing with Espresso
- **Integration Tests**: API integration tests

Run tests with:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # UI tests
```

### Code Style

The project uses:
- **ktlint**: Kotlin code formatting
- **Conventional Commits**: Git commit message format
- **Material 3**: UI design system

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Use meaningful commit messages (Conventional Commits)
- Add tests for new functionality
- Update documentation as needed
- Ensure accessibility compliance

## Security Considerations

- API keys are stored encrypted using EncryptedSharedPreferences
- Database is encrypted with SQLCipher
- No sensitive data is backed up to cloud services
- Network requests use HTTPS only
- Minimal app permissions required

## Privacy

- No user analytics or tracking
- All data stays on device
- No personal information collected
- Optional crash reporting (Firebase Crashlytics)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Check the [n8n documentation](https://docs.n8n.io/)
- Review the [Android documentation](https://developer.android.com/)
- Consultez la [documentation des scripts](scripts/README.md) pour les détails de déploiement

## Roadmap

### v1.1 (Planned)
- [ ] Widget support for home screen
- [ ] Export execution logs
- [ ] Search and filter workflows
- [ ] Manual workflow execution

### v1.2 (Future)
- [ ] Wear OS support
- [ ] Push notifications via webhooks
- [ ] Multi-instance support
- [ ] Advanced filtering options

### v2.0 (Long-term)
- [ ] Real-time updates
- [ ] Custom dashboards
- [ ] Team collaboration features
- [ ] Enterprise SSO support

## Acknowledgments

- [n8n](https://n8n.io/) - The workflow automation platform
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Material Design](https://material.io/) - Design system
- [Android Jetpack](https://developer.android.com/jetpack) - Android development libraries