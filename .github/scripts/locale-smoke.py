#!/usr/bin/env python3
"""Exercise localized screens and the real language picker through Android UI."""
import argparse
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

PACKAGE = 'com.doloreshazze.onesecond'
ROOT = Path(__file__).resolve().parents[2]


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, encoding='utf-8')


def start(locale=None):
    args = ['shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity']
    if locale:
        args += ['--es', 'smoke_locale', locale]
    output = adb(*args)
    assert 'Error:' not in output, output


def screen():
    adb('shell', 'uiautomator', 'dump', '/sdcard/one-second-locale.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/one-second-locale.xml'))


def expect(value):
    for _ in range(5):
        tree = screen()
        if any(value in node.get('text', '') or value in node.get('content-desc', '')
               for node in tree.iter('node')):
            return tree
        time.sleep(.4)
    raise AssertionError('Missing text: ' + value + '\n' + ET.tostring(tree, encoding='unicode'))


def tap_text(value):
    tree = expect(value)
    node = next(node for node in tree.iter('node') if node.get('text') == value)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def screenshot(name):
    path = ROOT / 'artifacts/screenshots' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p']))


def strings(folder):
    return {node.attrib['name']: (node.text or '').replace("\\'", "'").replace('\\"', '"')
            for node in ET.parse(ROOT / 'app/src/main/res' / folder / 'strings.xml').getroot()
            if node.tag == 'string'}


def settings(text):
    adb('shell', 'input', 'tap', '811', '293')
    expect(text['audio_settings'])
    for key in ('audio_music', 'audio_effects', 'language', 'privacy_policy'):
        expect(text[key])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--api', required=True)
    parser.add_argument('--release', action='store_true')
    args = parser.parse_args()
    if not args.release:
        for locale, folder in [('en', 'values'), ('ru', 'values-ru'), ('de', 'values-de'),
                               ('fr', 'values-fr'), ('es', 'values-es'), ('pt', 'values-pt'),
                               ('it', 'values-it'), ('pl', 'values-pl'), ('tr', 'values-tr'),
                               ('ar', 'values-ar'), ('hi', 'values-hi'), ('zh-CN', 'values-zh-rCN')]:
            adb('shell', 'pm', 'clear', PACKAGE)
            start(locale)
            text = strings(folder)
            expect(text['level_vase_title'])
            expect(text['level_vase_objective'])
            screenshot(f'api-{args.api}-{locale}-game.png')
            settings(text)
            screenshot(f'api-{args.api}-{locale}-settings.png')
            tap_text(text['privacy_policy'])
            expect(text['policy_title'])
            expect('daw81bk@gmail.com')
            print('Verified localized game, settings and offline policy:', locale, flush=True)

    # Check the real language picker in both the debug and production APKs.
    adb('shell', 'pm', 'clear', PACKAGE)
    start()
    english, german = strings('values'), strings('values-de')
    expect(english['level_vase_title'])
    settings(english)
    tap_text(english['language'])
    tap_text('Deutsch')
    expect(german['level_vase_title'])
    adb('shell', 'am', 'force-stop', PACKAGE)
    start()
    expect(german['level_vase_title'])
    settings(german)
    tap_text(german['privacy_policy'])
    expect(german['policy_title'])
    expect('daw81bk@gmail.com')
    screenshot(f'api-{args.api}-{"release" if args.release else "debug"}-privacy.png')
    print('Verified launch, persistent language choice and offline policy in',
          'release APK' if args.release else 'debug APK', flush=True)
