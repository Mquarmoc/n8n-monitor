#!/bin/bash

# App Signing Setup Script for n8n Monitor Android App
# This script helps configure app signing for release builds

set -e

echo "🔐 Setting up app signing configuration..."

# Configuration
KEYSTORE_DIR="$HOME/.android/keystores"
KEYSTORE_NAME="n8n-monitor-release.keystore"
KEYSTORE_PATH="$KEYSTORE_DIR/$KEYSTORE_NAME"
KEY_ALIAS="n8n-monitor"
KEYSTORE_PASSWORD=""
KEY_PASSWORD=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --keystore-path)
            KEYSTORE_PATH="$2"
            shift 2
            ;;
        --key-alias)
            KEY_ALIAS="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --keystore-path PATH   Custom keystore path [default: ~/.android/keystores/n8n-monitor-release.keystore]"
            echo "  --key-alias ALIAS      Key alias [default: n8n-monitor]"
            echo "  --help                 Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "❌ ERROR: keytool not found!"
    echo "Make sure Java JDK is installed and in PATH"
    exit 1
fi

echo "✅ keytool found"

# Create keystore directory
mkdir -p "$(dirname "$KEYSTORE_PATH")"

# Check if keystore already exists
if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  Keystore already exists at: $KEYSTORE_PATH"
    read -p "Do you want to use the existing keystore? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborted by user"
        exit 1
    fi
else
    echo "🔑 Creating new keystore..."
    
    # Prompt for passwords
    echo "Please enter keystore password (will be hidden):"
    read -s KEYSTORE_PASSWORD
    echo
    
    echo "Please enter key password (will be hidden, press Enter to use same as keystore):"
    read -s KEY_PASSWORD
    echo
    
    if [ -z "$KEY_PASSWORD" ]; then
        KEY_PASSWORD="$KEYSTORE_PASSWORD"
    fi
    
    # Generate keystore
    keytool -genkey -v -keystore "$KEYSTORE_PATH" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=n8n Monitor, OU=Development, O=n8n Monitor App, L=Unknown, S=Unknown, C=Unknown"
    
    echo "✅ Keystore created successfully"
fi

# Create or update keystore.properties
KEYSTORE_PROPERTIES="keystore.properties"
echo "📝 Creating keystore.properties..."

cat > "$KEYSTORE_PROPERTIES" << EOF
# Keystore configuration for n8n Monitor Android App
# This file contains sensitive information and should NOT be committed to version control

storeFile=$KEYSTORE_PATH
storePassword=\${KEYSTORE_PASSWORD}
keyAlias=$KEY_ALIAS
keyPassword=\${KEY_PASSWORD}
EOF

echo "✅ keystore.properties created"

# Add to .gitignore if it exists
if [ -f ".gitignore" ]; then
    if ! grep -q "keystore.properties" .gitignore; then
        echo "" >> .gitignore
        echo "# Keystore configuration (contains sensitive data)" >> .gitignore
        echo "keystore.properties" >> .gitignore
        echo "*.keystore" >> .gitignore
        echo "*.jks" >> .gitignore
        echo "✅ Added keystore files to .gitignore"
    else
        echo "✅ Keystore files already in .gitignore"
    fi
else
    echo "⚠️  .gitignore not found, please manually add keystore.properties to version control exclusions"
fi

# Update build.gradle.kts if needed
BUILD_GRADLE="app/build.gradle.kts"
if [ -f "$BUILD_GRADLE" ]; then
    echo "📝 Checking build.gradle.kts configuration..."
    
    if grep -q "signingConfigs" "$BUILD_GRADLE"; then
        echo "✅ Signing configuration already present in build.gradle.kts"
    else
        echo "⚠️  Signing configuration not found in build.gradle.kts"
        echo "Please add the following to your app/build.gradle.kts:"
        echo ""
        echo "android {"
        echo "    signingConfigs {"
        echo "        create(\"release\") {"
        echo "            val keystorePropertiesFile = rootProject.file(\"keystore.properties\")"
        echo "            if (keystorePropertiesFile.exists()) {"
        echo "                val keystoreProperties = Properties()"
        echo "                keystoreProperties.load(FileInputStream(keystorePropertiesFile))"
        echo "                storeFile = file(keystoreProperties[\"storeFile\"] as String)"
        echo "                storePassword = keystoreProperties[\"storePassword\"] as String"
        echo "                keyAlias = keystoreProperties[\"keyAlias\"] as String"
        echo "                keyPassword = keystoreProperties[\"keyPassword\"] as String"
        echo "            }"
        echo "        }"
        echo "    }"
        echo ""
        echo "    buildTypes {"
        echo "        release {"
        echo "            signingConfig = signingConfigs.getByName(\"release\")"
        echo "            // ... other release configuration"
        echo "        }"
        echo "    }"
        echo "}"
        echo ""
    fi
else
    echo "⚠️  app/build.gradle.kts not found"
fi

# Set environment variables for current session
echo "🔧 Setting up environment variables..."
echo "Please set these environment variables in your shell profile (~/.bashrc, ~/.zshrc, etc.):"
echo ""
echo "export KEYSTORE_PASSWORD='your_keystore_password'"
echo "export KEY_PASSWORD='your_key_password'"
echo ""
echo "Or create a .env file (not committed) with:"
echo "KEYSTORE_PASSWORD=your_keystore_password"
echo "KEY_PASSWORD=your_key_password"

# Security recommendations
echo ""
echo "🔒 Security recommendations:"
echo "  ✅ Store keystore in a secure location (backed up separately)"
echo "  ✅ Use strong passwords for keystore and key"
echo "  ✅ Never commit keystore.properties or .keystore files"
echo "  ✅ Use environment variables for passwords in CI/CD"
echo "  ✅ Keep a backup of your keystore in a secure location"
echo "  ⚠️  If you lose your keystore, you cannot update your app on Google Play!"

echo ""
echo "🎉 App signing setup completed!"
echo "📍 Keystore location: $KEYSTORE_PATH"
echo "🔑 Key alias: $KEY_ALIAS"
echo "📝 Configuration file: $KEYSTORE_PROPERTIES"
echo ""
echo "Next steps:"
echo "  1. Set environment variables for passwords"
echo "  2. Update app/build.gradle.kts with signing configuration (if not done)"
echo "  3. Test release build: ./gradlew assembleRelease"
echo "  4. Backup your keystore securely!"