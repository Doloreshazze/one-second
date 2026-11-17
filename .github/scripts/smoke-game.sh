#!/usr/bin/env bash
set -euo pipefail

mkdir -p artifacts/screenshots
adb shell wm size 900x1900
adb shell wm density 320
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install "$1"
adb shell am start -W -n com.doloreshazze.onesecond/.MainActivity

expect_screen() {
  local title="$1"
  local xml=""
  for attempt in {1..6}; do
    if adb shell uiautomator dump /sdcard/one-second-screen.xml >/dev/null 2>&1; then
      xml="$(adb shell cat /sdcard/one-second-screen.xml)"
      if [[ "$xml" == *"$title"* ]]; then
        echo "Verified: $title"
        return
      fi
    fi
    sleep 1
  done
  echo "Expected screen: $title"
  echo "$xml"
  adb logcat -d -s AndroidRuntime
  return 1
}

screenshot() {
  adb exec-out screencap -p > "artifacts/screenshots/$1.png"
  test -s "artifacts/screenshots/$1.png"
}

tap_action() {
  # The game frame is centered inside the system bar insets at this screen size.
  adb shell input tap 739 1662
}

expect_screen 'Спаси вазу'
screenshot vase
adb shell input swipe 300 900 300 1060 180
expect_screen 'Ваза разбилась'
tap_action
expect_screen 'Спаси вазу'
adb shell input swipe 300 900 400 1038 180
expect_screen 'Ваза спасена'
screenshot vase-saved
tap_action
expect_screen 'Не разбуди кота'
screenshot cat
adb shell input swipe 300 1000 440 1000 180
expect_screen 'Кот проснулся!'
screenshot cat-awake
tap_action
expect_screen 'Не разбуди кота'
adb shell input swipe 300 1000 440 902 180
expect_screen 'Рыбка спасена'
screenshot fish-saved
tap_action
expect_screen 'Спаси вазу'
