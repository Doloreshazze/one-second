#!/usr/bin/env bash
set -euo pipefail

mkdir -p artifacts/screenshots
screen=artifacts/screen.xml
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
        echo "$xml" > "$screen"
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

pick_level() {
  adb shell input tap 779 201
  expect_screen "$1"
  local x y
  read -r x y < <(python3 - "$screen" "$1" <<'PY'
import re
import sys
from xml.etree import ElementTree as ET

root = ET.parse(sys.argv[1])
row = next(node for node in root.iter("node") if node.get("text") == sys.argv[2])
x1, y1, x2, y2 = map(int, re.findall(r"[0-9]+", row.get("bounds")))
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  )
  adb shell input tap "$x" "$y"
}

while IFS=$'\t' read -r level title outcome result end_x end_y action; do
  expect_screen "$title"
  if [[ ! -s "artifacts/screenshots/$level.png" ]]; then
    screenshot "$level"
  fi
  adb shell input swipe 300 900 "$end_x" "$end_y" 180
  expect_screen "$result"
  case "$outcome" in
    drop_caught|paper_saved|photo_taken|train_derailed|clock_started) screenshot "$outcome" ;;
  esac
  if [[ "$action" == drop-again ]]; then
    pick_level '3. Последняя капля'
  else
    tap_action
  fi
done < "$2"

expect_screen 'Спаси вазу'
pick_level '9. Сердце мастерской'
expect_screen 'Сердце мастерской'
adb shell am force-stop com.doloreshazze.onesecond
adb shell am start -W -n com.doloreshazze.onesecond/.MainActivity
expect_screen 'Сердце мастерской'
