#!/usr/bin/env bash
set -euo pipefail

mkdir -p artifacts/screenshots
screen=artifacts/screen.xml
adb shell wm size 900x1900
adb shell wm density 320
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r "$1"
adb shell am start -W -n com.doloreshazze.onesecond/.MainActivity --es smoke_locale ru

expect_screen() {
  local title="$1"
  local xml=""
  local dump_output=""
  for attempt in {1..6}; do
    adb shell rm -f /data/local/tmp/one-second-screen.xml
    dump_output="$(adb shell uiautomator dump /data/local/tmp/one-second-screen.xml 2>&1 || true)"
    if xml="$(adb shell cat /data/local/tmp/one-second-screen.xml 2>/dev/null)" && [[ -n "$xml" ]]; then
      if [[ "$xml" == *"$title"* ]]; then
        echo "Verified: $title"
        echo "$xml" > "$screen"
        return
      fi
    fi
    sleep 1
  done
  echo "Expected screen: $title"
  echo "$dump_output"
  echo "$xml"
  adb exec-out screencap -p > artifacts/screenshots/failure.png
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
  expect_screen 'Уровни'
  for page in {1..3}; do
    if grep -Fq "text=\"$1\"" "$screen"; then break; fi
    adb shell input swipe 450 1450 450 640 250
    expect_screen 'Уровни'
  done
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

# Keep the gesture list on a separate descriptor. adb shell can read its stdin.
while IFS=$'\t' read -r -u 3 level title outcome result end_x end_y action; do
  expect_screen "$title"
  if [[ ! -s "artifacts/screenshots/$level.png" ]]; then
    screenshot "$level"
  fi
  adb shell input swipe 300 900 "$end_x" "$end_y" 180
  expect_screen "$result"
  case "$outcome" in
    drop_caught|paper_saved|photo_taken|clock_started|weight_added|weight_removed|dragon_shadow|hare_shadow|band_played|boat_docked|fair_open)
      screenshot "$outcome" ;;
  esac
  if [[ "$action" == replay-* ]]; then
    pick_level "$(cut -d - -f 2 <<< "$action"). $title"
  else
    tap_action
  fi
done 3< "$2"

expect_screen 'Спаси вазу'
pick_level '19. Вторая секунда'
expect_screen 'Вторая секунда'
adb shell am force-stop com.doloreshazze.onesecond
adb shell am start -W -n com.doloreshazze.onesecond/.MainActivity --es smoke_locale ru
expect_screen 'Вторая секунда'

open_audio_settings() {
  adb shell input tap 811 293
  expect_screen 'Настройки'
}

audio_control() {
  local coords
  coords="$(python3 - "$screen" "$1" "$2" <<'PY'
import re
import sys
from xml.etree import ElementTree as ET

path, name, expected = sys.argv[1:]
row = next(node for node in ET.parse(path).iter("node") if node.get("text") == name)
if expected:
    assert row.get("checked") == expected, f"{name}: expected checked={expected}"
x1, y1, x2, y2 = map(int, re.findall(r"[0-9]+", row.get("bounds")))
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  )"
  if [[ -n "${3:-}" ]]; then
    local x y
    read -r x y <<< "$coords"
    adb shell input tap "$x" "$y"
    expect_screen "$3"
  fi
}

open_audio_settings
screenshot audio_settings
audio_control 'Музыка' true 'Настройки'
audio_control 'Звуки' true
audio_control 'Звуки' true 'Настройки'
adb shell input keyevent KEYCODE_BACK
adb shell am force-stop com.doloreshazze.onesecond
adb shell am start -W -n com.doloreshazze.onesecond/.MainActivity --es smoke_locale ru
expect_screen 'Вторая секунда'
open_audio_settings
audio_control 'Музыка' false 'Настройки'
audio_control 'Звуки' false
audio_control 'Готово' '' 'Вторая секунда'
echo 'Verified: separate music and effects settings persist'
