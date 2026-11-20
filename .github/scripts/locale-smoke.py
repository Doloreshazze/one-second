#!/usr/bin/env python3
"""Exercise localized screens and the real language picker through Android UI."""
import argparse
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

PACKAGE = 'com.playeverywhere.onesecond'
ROOT = Path(__file__).resolve().parents[2]


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, encoding='utf-8')


def start():
    args = ['shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity']
    output = adb(*args)
    assert 'Error:' not in output, output


def screen():
    remote = '/data/local/tmp/one-second-locale.xml'
    last_error = None
    for _ in range(6):
        adb('shell', 'rm', '-f', remote)
        try:
            dump = adb('shell', 'uiautomator', 'dump', remote)
            xml = adb('shell', 'cat', remote)
            return ET.fromstring(xml)
        except (subprocess.CalledProcessError, ET.ParseError) as error:
            last_error = error
            time.sleep(.5)
    screenshot('failure-locale.png')
    raise AssertionError('Could not read Android UI: ' + str(last_error))


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
    tree = expect(text['audio_settings'])
    for key in ('audio_music', 'audio_effects', 'language', 'privacy_policy'):
        # Android 7 includes the switch state in its accessible text, such as
        # "Music ON". Later versions expose the state separately.
        present = any(node.get('text') == text[key]
                      or (key in ('audio_music', 'audio_effects')
                          and node.get('class') == 'android.widget.Switch'
                          and node.get('text', '').startswith(text[key] + ' '))
                      for node in tree.iter('node'))
        if not present:
            screenshot('failure-settings.png')
            raise AssertionError(key + '\n' + ET.tostring(tree, encoding='unicode'))


LANGUAGES = {'en': 'English', 'ru': 'Русский', 'de': 'Deutsch', 'fr': 'Français',
             'es': 'Español', 'pt': 'Português', 'it': 'Italiano', 'pl': 'Polski',
             'tr': 'Türkçe', 'ar': 'العربية', 'hi': 'हिन्दी', 'zh-CN': '简体中文'}


def change_language(locale, current):
    settings(current)
    tap_text(current['language'])
    name = LANGUAGES[locale]
    for _ in range(6):
        tree = screen()
        match = next((node for node in tree.iter('node') if node.get('text') == name), None)
        if match is not None:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', match.attrib['bounds']))
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
            return
        list_node = next(node for node in tree.iter('node') if node.get('scrollable') == 'true')
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', list_node.attrib['bounds']))
        x = (x1 + x2) // 2
        adb('shell', 'input', 'swipe', str(x), str(y2 - 30), str(x), str(y1 + 30), '300')
    raise AssertionError('Missing language option: ' + name)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--api', required=True)
    parser.add_argument('--release', action='store_true')
    parser.add_argument('--prepare')
    args = parser.parse_args()
    english, german = strings('values'), strings('values-de')
    if args.prepare:
        adb('shell', 'pm', 'clear', PACKAGE)
        start()
        expect(english['level_vase_title'])
        change_language(args.prepare, english)
        expect(strings('values-' + args.prepare)['level_vase_title'])
        print('Prepared game language:', args.prepare, flush=True)
        raise SystemExit(0)
    if not args.release:
        for locale, folder in [('en', 'values'), ('ru', 'values-ru'), ('de', 'values-de'),
                               ('fr', 'values-fr'), ('es', 'values-es'), ('pt', 'values-pt'),
                               ('it', 'values-it'), ('pl', 'values-pl'), ('tr', 'values-tr'),
                               ('ar', 'values-ar'), ('hi', 'values-hi'), ('zh-CN', 'values-zh-rCN')]:
            adb('shell', 'pm', 'clear', PACKAGE)
            start()
            expect(english['level_vase_title'])
            if locale != 'en':
                change_language(locale, english)
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
