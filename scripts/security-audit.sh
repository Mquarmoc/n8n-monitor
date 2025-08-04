#!/bin/bash

# Security Audit Script for n8n Monitor Android App
# This script checks for potential security issues in the codebase

set -e

echo "🔍 Starting security audit..."

# Check for hardcoded n8n API keys (excluding test files and legitimate usage)
echo "Checking for hardcoded n8n API keys..."
if grep -r "n8n.*key\|api.*key" app/src/main/ --include="*.kt" --include="*.xml" | grep -v "TODO\|FIXME\|XXX\|//.*key\|/\*.*key\|stringPreferencesKey\|placeholder\|settings_api_key\|apiKey =\|copy(apiKey"; then
    echo "❌ ERROR: Potential hardcoded API keys found!"
    grep -r "n8n.*key\|api.*key" app/src/main/ --include="*.kt" --include="*.xml" | grep -v "TODO\|FIXME\|XXX\|//.*key\|/\*.*key\|stringPreferencesKey\|placeholder\|settings_api_key\|apiKey =\|copy(apiKey"
    exit 1
fi
echo "✅ No hardcoded API keys found."

# Check for hardcoded secrets in test files
echo "Checking for hardcoded secrets in test files..."
if grep -r "password\|secret\|token" app/src/test/ --include="*.kt" | grep -v "TODO\|FIXME\|XXX\|//.*password\|/\*.*password"; then
    echo "❌ ERROR: Potential hardcoded secrets found in tests!"
    grep -r "password\|secret\|token" app/src/test/ --include="*.kt" | grep -v "TODO\|FIXME\|XXX\|//.*password\|/\*.*password"
    exit 1
fi
echo "✅ No hardcoded secrets found in tests."

# Check for hardcoded URLs (except localhost for testing and legitimate usage)
echo "Checking for hardcoded URLs..."
if grep -r "https://\|http://" app/src/main/ --include="*.kt" --include="*.xml" | grep -v "localhost\|127.0.0.1\|TODO\|FIXME\|XXX\|//.*http\|/\*.*http\|placeholder\|schemas.android.com\|your-n8n-instance.com"; then
    echo "❌ ERROR: Potential hardcoded URLs found!"
    grep -r "https://\|http://" app/src/main/ --include="*.kt" --include="*.xml" | grep -v "localhost\|127.0.0.1\|TODO\|FIXME\|XXX\|//.*http\|/\*.*http\|placeholder\|schemas.android.com\|your-n8n-instance.com"
    exit 1
fi
echo "✅ No hardcoded URLs found."

# Check ProGuard/R8 configuration
echo "Checking ProGuard/R8 configuration..."
if [ ! -f "app/proguard-rules.pro" ]; then
    echo "❌ ERROR: ProGuard/R8 rules file not found!"
    exit 1
fi
echo "✅ ProGuard/R8 configuration found."

# Check for debug logging in release builds
echo "Checking for debug logging..."
if grep -r "Log\.d\|Log\.v" app/src/main/ --include="*.kt" | grep -v "TODO\|FIXME\|XXX\|//.*Log\|/\*.*Log"; then
    echo "⚠️  WARNING: Debug logging found in main source code!"
    grep -r "Log\.d\|Log\.v" app/src/main/ --include="*.kt" | grep -v "TODO\|FIXME\|XXX\|//.*Log\|/\*.*Log"
fi
echo "✅ Debug logging check completed."

# Check R8 configuration
echo "Checking R8 configuration..."
if grep -q "android.enableR8.fullMode=false" gradle.properties; then
    echo "❌ ERROR: R8 full mode is disabled!"
    exit 1
fi
if grep -q "isMinifyEnabled = false" app/build.gradle.kts; then
    echo "❌ ERROR: R8 minification is disabled in release build!"
    exit 1
fi
echo "✅ R8 configuration check completed."

# Check for proper permission usage
echo "Checking for proper permission usage..."
if ! grep -q "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml; then
    echo "⚠️  WARNING: POST_NOTIFICATIONS permission not found in manifest!"
fi
if ! grep -q "INTERNET" app/src/main/AndroidManifest.xml; then
    echo "❌ ERROR: INTERNET permission not found in manifest!"
    exit 1
fi
echo "✅ Permission usage check completed."

# Check for SQLCipher usage
echo "Checking for database encryption..."
if ! grep -q "SQLCipher\|SupportFactory" app/src/main/ --include="*.kt"; then
    echo "⚠️  WARNING: SQLCipher database encryption not found!"
fi
echo "✅ Database encryption check completed."

# Check for proper backup rules
echo "Checking backup configuration..."
if [ ! -f "app/src/main/res/xml/backup_rules.xml" ]; then
    echo "❌ ERROR: Backup rules file not found!"
    exit 1
fi
if [ ! -f "app/src/main/res/xml/data_extraction_rules.xml" ]; then
    echo "❌ ERROR: Data extraction rules file not found!"
    exit 1
fi
echo "✅ Backup configuration check completed."

echo "🎉 Security audit completed successfully!"
echo "✅ No critical security issues found."