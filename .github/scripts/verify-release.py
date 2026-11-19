#!/usr/bin/env python3
"""Check translations, privacy boundaries and built distribution files."""
import argparse
import json
import re
import subprocess
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'


def strings(path):
    nodes = ET.parse(path).getroot().findall('string')
    values = {node.attrib['name']: node.text or '' for node in nodes}
    assert len(values) == len(nodes), f'Duplicate strings in {path}'
    return values


def verify_source():
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    assert not manifest.findall('uses-permission'), 'Offline game must not request permissions'
    app = manifest.find('application')
    assert app.get(ANDROID + 'allowBackup') == 'false'
    assert app.get(ANDROID + 'usesCleartextTraffic') == 'false'
    assert not app.findall('meta-data'), 'Unexpected SDK metadata'
    gradle = (ROOT / 'app/build.gradle').read_text(encoding='utf-8')
    assert re.search(r'targetSdk\s+36\b', gradle)
    assert re.search(r'versionCode\s+7\b', gradle)
    assert "versionName '1.5.0'" in gradle
    assert not re.search(r'\b(?:implementation|api|runtimeOnly)\s+[\'\"]', gradle)
    base = strings(RES / 'values/strings.xml')
    nontranslated = {node.attrib['name'] for node in
                     ET.parse(RES / 'values/strings.xml').getroot().findall('string')
                     if node.get('translatable') == 'false'}
    placeholders = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]')
    locale_folders = {'en': RES / 'values'}
    for folder in sorted(RES.glob('values-*/strings.xml')):
        locale = folder.parent.name.removeprefix('values-').replace('-r', '-')
        locale_folders[locale] = folder.parent
        values = strings(folder)
        assert values.keys() == base.keys() - nontranslated, (locale, 'Incomplete translation')
        for key, value in values.items():
            assert value.strip(), (locale, key, 'Empty translation')
            assert '\ufffd' not in value and '???' not in value, (locale, key, 'Invalid text')
            assert placeholders.findall(value) == placeholders.findall(base[key]), (locale, key, 'Format arguments')
    config = ET.parse(RES / 'xml/locales_config.xml').getroot()
    locales = {item.attrib[ANDROID + 'name'] for item in config}
    assert locales == locale_folders.keys(), (locales, locale_folders.keys())
    java_dir = ROOT / 'app/src/main/java/com/doloreshazze/onesecond'
    java = '\n'.join(path.read_text(encoding='utf-8') for path in java_dir.glob('*.java'))
    for name in re.findall(r'R\.string\.(\w+)', java):
        assert name in base, f'Missing R.string.{name}'
    assert not re.search(r'TestAds|MobileAds|com\.google\.(ads|firebase|android\.ump)|ca-app-pub-', java)
    locale_code = (java_dir / 'GameLocale.java').read_text(encoding='utf-8')
    tags = re.search(r'TAGS = \{([^}]+)', locale_code).group(1)
    assert {tag for tag in re.findall(r'"([^"]*)"', tags) if tag} == locales
    puzzle = (java_dir / 'PuzzleRound.java').read_text(encoding='utf-8')
    level_section = puzzle.split('enum Level {')[1].split(';')[0]
    outcome_section = puzzle.split('enum Outcome {')[1].split(';')[0]
    levels = re.findall(r'^\s+(\w+)\(', level_section, re.M)
    outcomes = re.findall(r'^\s+(\w+)\(', outcome_section, re.M)
    mappings = (java_dir / 'GameText.java').read_text(encoding='utf-8')
    for array, entries, prefix, suffix in [
            ('LEVEL_TITLES', levels, 'level_', '_title'),
            ('LEVEL_OBJECTIVES', levels, 'level_', '_objective'),
            ('OUTCOME_TITLES', outcomes, 'outcome_', '_title'),
            ('OUTCOME_DETAILS', outcomes, 'outcome_', '_detail')]:
        block = re.search(array + r' = \{([^}]+)', mappings).group(1)
        actual = re.findall(r'R\.string\.(\w+)', block)
        assert actual == [prefix + item.lower() + suffix for item in entries], array
    assert len(levels) == 19 and len(outcomes) == 45
    return {'version': '1.5.0', 'target_api': 36, 'levels': len(levels),
            'outcomes': len(outcomes), 'languages': sorted(locales),
            'strings_per_language': len(base) - len(nontranslated),
            'permissions': [], 'third_party_runtime_dependencies': []}


def verify_artifacts(directory, aapt):
    results = {}
    for path in sorted(Path(directory).glob('*')):
        if path.suffix not in {'.apk', '.aab'}:
            continue
        with zipfile.ZipFile(path) as archive:
            assert archive.testzip() is None, path
            names = archive.namelist()
            assert not any(name.endswith('.so') for name in names), 'Unexpected native library'
            for name in names:
                if name.endswith('.dex'):
                    dex = archive.read(name)
                    assert all(token not in dex for token in [b'Lcom/google/android/gms/ads/',
                               b'Lcom/google/android/ump/', b'Lcom/google/firebase/']), path
            if path.suffix == '.aab':
                assert 'BundleConfig.pb' in names and 'base/manifest/AndroidManifest.xml' in names
        result = {'native_libraries': 0, 'ad_or_analytics_classes': 0}
        if path.suffix == '.apk':
            badging = subprocess.check_output([aapt, 'dump', 'badging', str(path)], text=True)
            permissions = subprocess.check_output([aapt, 'dump', 'permissions', str(path)], text=True)
            assert "targetSdkVersion:'36'" in badging
            assert "versionName='1.5.0'" in badging and "versionCode='7'" in badging
            assert 'uses-permission' not in permissions
            if 'release' in path.name:
                assert 'application-debuggable' not in badging
            result['debuggable'] = 'application-debuggable' in badging
            result['permissions'] = []
        results[path.name] = result
    assert results, 'No APK or AAB artifacts found'
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifacts')
    parser.add_argument('--aapt')
    parser.add_argument('--output')
    args = parser.parse_args()
    result = verify_source()
    if args.artifacts:
        assert args.aapt
        result['artifacts'] = verify_artifacts(args.artifacts, args.aapt)
    rendered = json.dumps(result, ensure_ascii=False, indent=2)
    print(rendered)
    if args.output:
        Path(args.output).write_text(rendered + '\n', encoding='utf-8')
