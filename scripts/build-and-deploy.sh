#!/bin/bash

# Main Build and Deploy Script for n8n Monitor Android App
# This script orchestrates the complete build and deployment process

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "🚀 n8n Monitor Android App - Build and Deploy"
echo "============================================="
echo ""

# Default configuration
TRACK="internal"
SKIP_SETUP=false
SKIP_SECURITY=false
SKIP_TESTS=false
SKIP_BUILD=false
VERBOSE=false
SETUP_SIGNING=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --track)
            TRACK="$2"
            shift 2
            ;;
        --skip-setup)
            SKIP_SETUP=true
            shift
            ;;
        --skip-security)
            SKIP_SECURITY=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --setup-signing)
            SETUP_SIGNING=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --track TRACK         Upload track (internal, alpha, beta, production) [default: internal]"
            echo "  --skip-setup          Skip environment setup"
            echo "  --skip-security       Skip security audit"
            echo "  --skip-tests          Skip running tests"
            echo "  --skip-build          Skip build process"
            echo "  --setup-signing       Setup app signing configuration"
            echo "  --verbose             Enable verbose output"
            echo "  --help                Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                                    # Build and deploy to internal testing"
            echo "  $0 --track production                # Deploy to production"
            echo "  $0 --setup-signing                   # Setup app signing first"
            echo "  $0 --skip-tests --verbose            # Quick build with verbose output"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "📋 Configuration:"
echo "  Track: $TRACK"
echo "  Skip setup: $SKIP_SETUP"
echo "  Skip security: $SKIP_SECURITY"
echo "  Skip tests: $SKIP_TESTS"
echo "  Skip build: $SKIP_BUILD"
echo "  Setup signing: $SETUP_SIGNING"
echo "  Verbose: $VERBOSE"
echo ""

# Setup app signing if requested
if [ "$SETUP_SIGNING" = true ]; then
    echo "🔐 Setting up app signing..."
    if [ -f "scripts/setup-signing.sh" ]; then
        bash scripts/setup-signing.sh
    else
        echo "❌ ERROR: setup-signing.sh not found!"
        exit 1
    fi
    echo ""
fi

# Environment setup
if [ "$SKIP_SETUP" = false ]; then
    echo "🔧 Setting up environment..."
    
    # Setup JDK 17
    if [ -f "scripts/jdk17-wrapper.sh" ]; then
        echo "☕ Configuring JDK 17..."
        source scripts/jdk17-wrapper.sh
    else
        echo "⚠️  WARNING: jdk17-wrapper.sh not found"
    fi
    
    echo "✅ Environment setup completed"
    echo ""
else
    echo "⏭️  Skipping environment setup"
    echo ""
fi

# Security audit
if [ "$SKIP_SECURITY" = false ]; then
    echo "🔒 Running security audit..."
    if [ -f "scripts/security-audit.sh" ]; then
        bash scripts/security-audit.sh
        echo "✅ Security audit passed"
    else
        echo "❌ ERROR: security-audit.sh not found!"
        exit 1
    fi
    echo ""
else
    echo "⏭️  Skipping security audit"
    echo ""
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
    echo ""
else
    echo "⏭️  Skipping tests"
    echo ""
fi

# Build and deploy
if [ "$SKIP_BUILD" = false ]; then
    echo "📦 Building and deploying..."
    
    # Prepare upload script arguments
    UPLOAD_ARGS="--track $TRACK"
    if [ "$SKIP_TESTS" = true ]; then
        UPLOAD_ARGS="$UPLOAD_ARGS --skip-tests"
    fi
    if [ "$VERBOSE" = true ]; then
        UPLOAD_ARGS="$UPLOAD_ARGS --verbose"
    fi
    
    # Run upload script
    if [ -f "scripts/upload-aab.sh" ]; then
        bash scripts/upload-aab.sh $UPLOAD_ARGS
    else
        echo "❌ ERROR: upload-aab.sh not found!"
        exit 1
    fi
else
    echo "⏭️  Skipping build and deploy"
fi

echo ""
echo "🎉 Build and deploy process completed successfully!"
echo ""
echo "📱 Your app has been deployed to the '$TRACK' track"
echo "🌐 Check the Google Play Console: https://play.google.com/console/developers"
echo ""
echo "📋 Summary:"
echo "  ✅ Environment configured"
if [ "$SKIP_SECURITY" = false ]; then
    echo "  ✅ Security audit passed"
fi
if [ "$SKIP_TESTS" = false ]; then
    echo "  ✅ Tests passed"
fi
if [ "$SKIP_BUILD" = false ]; then
    echo "  ✅ AAB built and uploaded to $TRACK"
fi
echo ""
echo "🚀 Next steps:"
case $TRACK in
    "internal")
        echo "  1. Test the app with internal testers"
        echo "  2. Promote to alpha when ready: $0 --track alpha"
        ;;
    "alpha")
        echo "  1. Test the app with alpha testers"
        echo "  2. Promote to beta when ready: $0 --track beta"
        ;;
    "beta")
        echo "  1. Test the app with beta testers"
        echo "  2. Promote to production when ready: $0 --track production"
        ;;
    "production")
        echo "  1. Monitor the production release"
        echo "  2. Celebrate! 🎉"
        ;;
esac
echo ""
echo "✨ Happy deploying!"