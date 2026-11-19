#!/usr/bin/env bash
set -euo pipefail
api="$1"
mkdir -p artifacts/screenshots
trap 'adb logcat -d -s AndroidRuntime > "artifacts/android-log-api-$api.txt" || true' EXIT
adb logcat -c
adb shell wm size 900x1900
adb shell wm density 320
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
if [[ "$api" == 36 ]]; then
  bash .github/scripts/smoke-game.sh artifacts/OneSecond-debug.apk artifacts/smoke-shots.tsv
else
  adb install -r artifacts/OneSecond-debug.apk
fi
python3 .github/scripts/locale-smoke.py --api "$api"

# This disposable key only installs the production build on the test emulator.
# The unsigned distribution files remain untouched. It is not an upload key.
tools="$ANDROID_HOME/build-tools/36.0.0"
if [[ ! -x "$tools/apksigner" ]]; then
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;36.0.0"
fi
key_dir="$(mktemp -d)"
keytool -genkeypair -keystore "$key_dir/emulator.jks" -alias smoke \
  -storepass emulator-only -keypass emulator-only -keyalg RSA -keysize 2048 \
  -validity 2 -dname "CN=Disposable emulator test" >/dev/null 2>&1
"$tools/apksigner" sign --ks "$key_dir/emulator.jks" --ks-pass pass:emulator-only \
  --out artifacts/OneSecond-release-test.apk artifacts/distribution/OneSecond-1.5.0-release-unsigned.apk
adb uninstall com.doloreshazze.onesecond
adb install artifacts/OneSecond-release-test.apk
python3 .github/scripts/locale-smoke.py --api "$api" --release
adb logcat -d -s AndroidRuntime > "artifacts/android-log-api-$api.txt"
if grep -Fq 'Process: com.doloreshazze.onesecond' "artifacts/android-log-api-$api.txt"; then
  cat "artifacts/android-log-api-$api.txt"
  exit 1
fi
