#!/bin/bash

# Quick validation script for Phase 1 setup

echo "==================================="
echo "Phase 1 Validation Script"
echo "==================================="
echo ""

# Check Java version
echo "Checking Java version..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "✅ Java installed: $java_version"
else
    echo "❌ Java not found. Please install JDK 17 or higher."
    exit 1
fi

# Check PostgreSQL
echo ""
echo "Checking PostgreSQL..."
if command -v psql &> /dev/null; then
    pg_version=$(psql --version | awk '{print $3}')
    echo "✅ PostgreSQL installed: $pg_version"
else
    echo "⚠️  PostgreSQL not found. Install PostgreSQL 15 for server."
fi

# Check server structure
echo ""
echo "Checking server structure..."
if [ -f "server/build.gradle.kts" ]; then
    echo "✅ Server build.gradle.kts found"
else
    echo "❌ Server build.gradle.kts not found"
fi

if [ -f "server/.env.example" ]; then
    echo "✅ Server .env.example found"
    if [ -f "server/.env" ]; then
        echo "✅ Server .env configured"
    else
        echo "⚠️  Server .env not found. Copy from .env.example"
    fi
else
    echo "❌ Server .env.example not found"
fi

# Check Android structure
echo ""
echo "Checking Android structure..."
if [ -f "android/build.gradle.kts" ]; then
    echo "✅ Android build.gradle.kts found"
else
    echo "❌ Android build.gradle.kts not found"
fi

if [ -f "android/app/google-services.json.example" ]; then
    echo "✅ Android google-services.json.example found"
    if [ -f "android/app/google-services.json" ]; then
        echo "✅ Android google-services.json configured"
    else
        echo "⚠️  Android google-services.json not found. Copy from example"
    fi
else
    echo "❌ Android google-services.json.example not found"
fi

# Check Kotlin files
echo ""
echo "Checking Kotlin files..."
server_kt_count=$(find server/src -name "*.kt" 2>/dev/null | wc -l)
android_kt_count=$(find android/app/src -name "*.kt" 2>/dev/null | wc -l)
echo "✅ Server Kotlin files: $server_kt_count"
echo "✅ Android Kotlin files: $android_kt_count"

# Summary
echo ""
echo "==================================="
echo "Validation Complete!"
echo "==================================="
echo ""
echo "Next steps:"
echo "1. Configure server/.env with database credentials"
echo "2. Configure android/app/google-services.json"
echo "3. Create PostgreSQL database: dune_ttrpg"
echo "4. Run server: cd server && ./gradlew run"
echo "5. Open android/ in Android Studio and run"
echo ""
echo "See PHASE1-VALIDATION.md for detailed instructions."
