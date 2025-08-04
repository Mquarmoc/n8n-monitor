#!/bin/bash

# JDK 17 Wrapper Script for n8n Monitor Android App
# This script ensures the project uses JDK 17 for builds

set -e

echo "🔧 Setting up JDK 17 environment..."

# Function to detect OS
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        CYGWIN*|MINGW*|MSYS*) echo "windows";;
        *)          echo "unknown";;
    esac
}

# Function to find JDK 17
find_jdk17() {
    local os=$(detect_os)
    local jdk_paths=()
    
    case $os in
        "linux")
            jdk_paths=(
                "/usr/lib/jvm/java-17-openjdk"
                "/usr/lib/jvm/java-17-openjdk-amd64"
                "/usr/lib/jvm/adoptopenjdk-17-hotspot"
                "/opt/jdk-17"
                "$HOME/.sdkman/candidates/java/17*"
            )
            ;;
        "macos")
            jdk_paths=(
                "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
                "/Library/Java/JavaVirtualMachines/adoptopenjdk-17.jdk/Contents/Home"
                "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
                "$HOME/.sdkman/candidates/java/17*"
            )
            ;;
        "windows")
            jdk_paths=(
                "C:/Program Files/Eclipse Adoptium/jdk-17*"
                "C:/Program Files/OpenJDK/openjdk-17*"
                "C:/Program Files/Java/jdk-17*"
            )
            ;;
    esac
    
    for path in "${jdk_paths[@]}"; do
        if [ -d "$path" ] && [ -x "$path/bin/java" ]; then
            echo "$path"
            return 0
        fi
    done
    
    return 1
}

# Check if JAVA_HOME is already set to JDK 17
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    java_version=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" = "17" ]; then
        echo "✅ JDK 17 already configured via JAVA_HOME: $JAVA_HOME"
        exit 0
    fi
fi

# Try to find JDK 17
echo "🔍 Searching for JDK 17..."
jdk17_path=$(find_jdk17)

if [ -z "$jdk17_path" ]; then
    echo "❌ ERROR: JDK 17 not found!"
    echo "Please install JDK 17 from one of these sources:"
    echo "  - OpenJDK: https://openjdk.org/projects/jdk/17/"
    echo "  - Eclipse Temurin: https://adoptium.net/temurin/releases/?version=17"
    echo "  - Oracle JDK: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html"
    echo "  - SDKMAN: sdk install java 17.0.9-tem"
    exit 1
fi

echo "✅ Found JDK 17 at: $jdk17_path"

# Export JAVA_HOME
export JAVA_HOME="$jdk17_path"
export PATH="$JAVA_HOME/bin:$PATH"

echo "🎯 JAVA_HOME set to: $JAVA_HOME"
echo "🎯 Java version: $(java -version 2>&1 | head -n 1)"

# Verify Gradle compatibility
echo "🔧 Verifying Gradle compatibility..."
if [ -f "./gradlew" ]; then
    echo "✅ Gradle wrapper found"
    ./gradlew --version
else
    echo "❌ ERROR: Gradle wrapper not found!"
    exit 1
fi

echo "🚀 JDK 17 environment ready for Android build!"
echo "You can now run: ./gradlew assembleRelease"