#!/bin/bash

# Upload AAB Script for n8n Monitor Android App
# This script builds and uploads the Android App Bundle to Google Play Store

set -e

echo "📦 Starting AAB build and upload process..."

# Configuration
TRACK="internal"  # Default track: internal, alpha, beta, production
SKIP_BUILD=false
SKIP_TESTS=false
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --track)
            TRACK="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --track TRACK      Upload track (internal, alpha, beta, production) [default: internal]"
            echo "  --skip-build       Skip the build process and use existing AAB"
            echo "  --skip-tests       Skip running tests before build"
            echo "  --verbose          Enable verbose output"
            echo "  --help             Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate track
case $TRACK in
    internal|alpha|beta|production)
        echo "✅ Using track: $TRACK"
        ;;
    *)
        echo "❌ ERROR: Invalid track '$TRACK'. Must be one of: internal, alpha, beta, production"
        exit 1
        ;;
esac

# Check required environment variables
echo "🔍 Checking environment variables..."
if [ -z "$GOOGLE_PLAY_JSON_KEY_PATH" ] && [ -z "$GOOGLE_PLAY_JSON_KEY_DATA" ]; then
    echo "❌ ERROR: Google Play service account key not configured!"
    echo "Please set either:"
    echo "  GOOGLE_PLAY_JSON_KEY_PATH - path to service account JSON file"
    echo "  GOOGLE_PLAY_JSON_KEY_DATA - service account JSON data"
    exit 1
fi

if [ -n "$GOOGLE_PLAY_JSON_KEY_PATH" ] && [ ! -f "$GOOGLE_PLAY_JSON_KEY_PATH" ]; then
    echo "❌ ERROR: Google Play service account JSON file not found: $GOOGLE_PLAY_JSON_KEY_PATH"
    exit 1
fi

echo "✅ Google Play credentials configured"

# Check if fastlane is installed
if ! command -v fastlane &> /dev/null; then
    echo "❌ ERROR: fastlane is not installed!"
    echo "Install it with: gem install fastlane"
    exit 1
fi

echo "✅ fastlane is installed"

# Setup JDK 17
echo "🔧 Setting up JDK 17..."
if [ -f "scripts/jdk17-wrapper.sh" ]; then
    source scripts/jdk17-wrapper.sh
else
    echo "⚠️  WARNING: JDK 17 wrapper script not found, assuming JDK 17 is already configured"
fi

# Run security audit
if [ "$SKIP_TESTS" = false ]; then
    echo "🔒 Running security audit..."
    if [ -f "scripts/security-audit.sh" ]; then
        bash scripts/security-audit.sh
    else
        echo "⚠️  WARNING: Security audit script not found"
    fi
fi

# Run tests
if [ "$SKIP_TESTS" = false ]; then
    echo "🧪 Running tests..."
    if [ "$VERBOSE" = true ]; then
        ./gradlew test --info
    else
        ./gradlew test
    fi
    echo "✅ Tests passed"
fi

# Build AAB
if [ "$SKIP_BUILD" = false ]; then
    echo "🏗️  Building release AAB..."
    if [ "$VERBOSE" = true ]; then
        ./gradlew bundleRelease --info
    else
        ./gradlew bundleRelease
    fi
    echo "✅ AAB built successfully"
else
    echo "⏭️  Skipping build, using existing AAB"
fi

# Check if AAB exists
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
if [ ! -f "$AAB_PATH" ]; then
    echo "❌ ERROR: AAB file not found at $AAB_PATH"
    echo "Make sure the build completed successfully"
    exit 1
fi

echo "✅ AAB found at: $AAB_PATH"
echo "📊 AAB size: $(du -h "$AAB_PATH" | cut -f1)"

# Upload to Google Play Store
echo "🚀 Uploading to Google Play Store ($TRACK track)..."
case $TRACK in
    internal)
        fastlane android internal
        ;;
    alpha)
        fastlane android alpha
        ;;
    beta)
        fastlane android beta
        ;;
    production)
        fastlane android production
        ;;
esac

echo "✅ Upload completed successfully!"
echo "🎉 AAB has been uploaded to Google Play Store ($TRACK track)"
echo "📱 You can now test the app or promote it to the next track in the Google Play Console"

# Optional: Open Google Play Console
if command -v open &> /dev/null; then
    echo "🌐 Opening Google Play Console..."
    open "https://play.google.com/console/developers"
elif command -v xdg-open &> /dev/null; then
    echo "🌐 Opening Google Play Console..."
    xdg-open "https://play.google.com/console/developers"
fi