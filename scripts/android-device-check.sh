#!/usr/bin/env bash
set -uo pipefail
result=0
android/gradlew -p android :app:connectedDebugAndroidTest --console=plain || result=$?
mkdir -p android-test-results
adb pull /sdcard/Android/data/info.cosmonet.flusiva/files/screenshots android-test-results/ || true
adb logcat -d -s AndroidRuntime > android-test-results/android-runtime.txt || true
exit "$result"
