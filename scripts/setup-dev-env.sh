#!/bin/bash

# Development Environment Setup Script for n8n Monitor Android App
# This script sets up the complete development environment

set -e

echo "🚀 Setting up n8n Monitor Android App development environment..."

# Function to detect OS
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        CYGWIN*|MINGW*|MSYS*) echo "windows";;
        *)          echo "unknown";;
    esac
}

OS=$(detect_os)
echo "🖥️  Detected OS: $OS"

# Check if running in project directory
if [ ! -f "build.gradle.kts" ] || [ ! -f "settings.gradle.kts" ]; then
    echo "❌ ERROR: This script must be run from the project root directory!"
    exit 1
fi

# Check Java installation
echo "☕ Checking Java installation..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "✅ Java $java_version found"
    if [ "$java_version" != "17" ]; then
        echo "⚠️  WARNING: Java 17 is recommended for this project"
        echo "Current version: $java_version"
    fi
else
    echo "❌ ERROR: Java not found!"
    echo "Please install JDK 17 from: https://adoptium.net/temurin/releases/?version=17"
    exit 1
fi

# Check Android SDK
echo "📱 Checking Android SDK..."
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK found at: $ANDROID_HOME"
else
    echo "❌ ERROR: Android SDK not found!"
    echo "Please install Android Studio and set ANDROID_HOME environment variable"
    echo "Download from: https://developer.android.com/studio"
    exit 1
fi

# Check Ruby installation (for fastlane)
echo "💎 Checking Ruby installation..."
if command -v ruby &> /dev/null; then
    ruby_version=$(ruby -v | cut -d' ' -f2)
    echo "✅ Ruby $ruby_version found"
else
    echo "❌ ERROR: Ruby not found!"
    case $OS in
        "macos")
            echo "Install with: brew install ruby"
            ;;
        "linux")
            echo "Install with: sudo apt-get install ruby-full (Ubuntu/Debian)"
            echo "Or: sudo yum install ruby (CentOS/RHEL)"
            ;;
        "windows")
            echo "Download from: https://rubyinstaller.org/"
            ;;
    esac
    exit 1
fi

# Check Bundler
echo "📦 Checking Bundler..."
if command -v bundle &> /dev/null; then
    echo "✅ Bundler found"
else
    echo "📦 Installing Bundler..."
    gem install bundler
fi

# Install Ruby dependencies
echo "📦 Installing Ruby dependencies..."
if [ -f "Gemfile" ]; then
    bundle install
    echo "✅ Ruby dependencies installed"
else
    echo "⚠️  WARNING: Gemfile not found, installing fastlane directly"
    gem install fastlane
fi

# Check fastlane installation
echo "🚀 Checking fastlane installation..."
if command -v fastlane &> /dev/null; then
    fastlane_version=$(fastlane --version | head -n 1 | cut -d' ' -f2)
    echo "✅ fastlane $fastlane_version installed"
else
    echo "❌ ERROR: fastlane installation failed!"
    exit 1
fi

# Make scripts executable
echo "🔧 Setting up scripts..."
chmod +x scripts/*.sh
echo "✅ Scripts made executable"

# Validate Gradle wrapper
echo "🔧 Validating Gradle wrapper..."
if [ -f "gradlew" ]; then
    chmod +x gradlew
    ./gradlew --version
    echo "✅ Gradle wrapper validated"
else
    echo "❌ ERROR: Gradle wrapper not found!"
    exit 1
fi

# Run initial security audit
echo "🔒 Running initial security audit..."
if [ -f "scripts/security-audit.sh" ]; then
    bash scripts/security-audit.sh
    echo "✅ Security audit completed"
else
    echo "⚠️  WARNING: Security audit script not found"
fi

# Test build
echo "🏗️  Testing build..."
./gradlew assembleDebug
echo "✅ Debug build successful"

# Setup Git hooks (optional)
echo "🔗 Setting up Git hooks..."
if [ -d ".git" ]; then
    # Pre-commit hook for security audit
    cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
echo "Running security audit..."
if [ -f "scripts/security-audit.sh" ]; then
    bash scripts/security-audit.sh
else
    echo "Security audit script not found, skipping..."
fi
EOF
    chmod +x .git/hooks/pre-commit
    echo "✅ Git pre-commit hook installed"
else
    echo "⚠️  WARNING: Not a Git repository, skipping Git hooks setup"
fi

echo ""
echo "🎉 Development environment setup completed successfully!"
echo ""
echo "📋 Next steps:"
echo "  1. Configure Google Play Console service account (for releases)"
echo "  2. Set environment variables:"
echo "     - GOOGLE_PLAY_JSON_KEY_PATH or GOOGLE_PLAY_JSON_KEY_DATA"
echo "  3. Update app signing configuration in app/build.gradle.kts"
echo ""
echo "🚀 Available commands:"
echo "  ./gradlew assembleDebug          - Build debug APK"
echo "  ./gradlew bundleRelease          - Build release AAB"
echo "  fastlane android test            - Run tests"
echo "  fastlane android internal        - Deploy to internal testing"
echo "  bash scripts/upload-aab.sh       - Build and upload AAB"
echo "  bash scripts/security-audit.sh   - Run security audit"
echo ""
echo "📚 Documentation:"
echo "  - Fastlane: https://docs.fastlane.tools/"
echo "  - Android App Bundles: https://developer.android.com/guide/app-bundle"
echo "  - Google Play Console: https://play.google.com/console/"
echo ""
echo "✨ Happy coding!"