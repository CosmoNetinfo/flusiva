#!/usr/bin/env bash
set -uo pipefail
result=0
android/gradlew -p android :app:connectedDebugAndroidTest --console=plain || result=$?
mkdir -p android-test-results
adb pull /sdcard/Download/Flusiva-tests android-test-results/screenshots || true
adb logcat -d -s AndroidRuntime > android-test-results/android-runtime.txt || true
exit "$result"
