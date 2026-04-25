#!/bin/sh
# IndoLearn Gradle wrapper
# Usage: ./gradlew assembleDebug
set -e

# Check for gradle in PATH, or ANDROID_HOME gradle
if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/../gradle/current/bin/gradle" ]; then
    "$ANDROID_HOME/../gradle/current/bin/gradle" "$@"
else
    echo "Gradle not found. Install Android Studio or run: brew install gradle"
    exit 1
fi
