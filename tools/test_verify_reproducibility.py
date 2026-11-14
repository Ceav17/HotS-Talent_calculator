"""Isolated CLI/setup tests: no real downloads, SDK changes, keys or Gradle builds."""

import hashlib
import io
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

from android_sdk_bootstrap import latest_linux_archive

ROOT = Path(__file__).resolve().parents[1]


def metadata(archive, *, channel="channel-0", checksum=None):
    checksum = checksum or hashlib.sha1(archive).hexdigest()
    return f'''<repository><remotePackage path="cmdline-tools;latest">
      <revision><major>99</major></revision><channelRef ref="{channel}"/>
      <archives><archive><host-os>linux</host-os><complete>
      <size>{len(archive)}</size><checksum type="sha1">{checksum}</checksum>
      <url>commandlinetools-linux-99999999_latest.zip</url>
      </complete></archive></archives></remotePackage></repository>'''


SDK_MANAGER = '''import json, os, sys
from pathlib import Path
args = sys.argv[1:]
with open(os.environ['VERIFY_TEST_EVENTS'], 'a') as log:
    log.write(json.dumps(args) + '\\n')
modern = Path(sys.argv[0]).name == 'android'
root_arg = '--sdk=' if modern else '--sdk_root='
assert args[0].startswith(root_arg), args
sdk = Path(args[0].split('=', 1)[1])
listing = 'list' in args if modern else '--list' in args
if listing:
    if modern:
        assert args[1:] == ['sdk', 'list', '--all'], args
    print('platforms;' + os.environ.get('VERIFY_TEST_PLATFORM', 'android-37.0') + ' | 1 | SDK Platform')
    sys.exit(0)
if modern:
    assert args[1:4] == ['sdk', '--ignore-outdated-xmls', 'install'], args
else:
    assert args[1] == '--install', args
print('Mock license: type y to install', flush=True)
if sys.stdin.readline().strip() != 'y':
    sys.exit(0)
for package in args:
    if package.startswith('platforms;'):
        target = sdk / 'platforms' / package.split(';')[1]
        target.mkdir(parents=True, exist_ok=True)
        (target / 'android.jar').write_bytes(b'fixture')
        (target / 'source.properties').write_text('Pkg.Revision=1')
    elif package.startswith('build-tools;'):
        target = sdk / 'build-tools' / package.split(';')[1]
        target.mkdir(parents=True, exist_ok=True)
        for name in ('aapt2', 'apksigner', 'zipalign'):
            tool = target / name
            tool.write_bytes(Path(os.environ['VERIFY_TEST_TOOL']).read_bytes())
            tool.chmod(0o755)
'''

SDK_TOOL = '''import os, sys
from pathlib import Path
if Path(sys.argv[0]).name == 'aapt2':
    print("package: name='com.example.hots' versionCode='1'")
elif Path(sys.argv[0]).name == 'apksigner':
    print('Verifies')
    sys.exit(int(os.environ.get('VERIFY_TEST_SIGNATURE_EXIT', '0')))
'''


class VerifierSetupTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='.verify-test-', dir=ROOT)
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.project = self.base / 'source with spaces'
        self.project.mkdir()
        self.sdk = self.base / 'sdk with spaces'
        self.bin = self.base / 'bin'
        self.bin.mkdir()
        self.modules = self.base / 'modules'
        self.modules.mkdir()
        self.jdk = self.base / 'jdk'
        self.events = self.base / 'sdk-events'
        self.network = self.base / 'network-events'
        self.apk = self.base / 'signed fixture.apk'
        self.apk.write_bytes(b'APK fixture; SDK parsing/signature tools are mocked')
        self.script = self.project / 'verify-reproducibility.sh'
        shutil.copy2(ROOT / self.script.name, self.script)
        self.write('tools/verify_apk_reproducibility.py', '# unused by preflight tests\n')
        shutil.copy2(ROOT / 'tools/android_sdk_bootstrap.py', self.project / 'tools/android_sdk_bootstrap.py')
        for name in ('gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties', 'settings.gradle.kts'):
            self.write(name, 'fixture')
        self.write('app/build.gradle.kts', 'version = release(37)\napplicationId = "com.example.hots"\nversionCode = 1\n')
        self.write('gradle.properties', 'hots.buildToolsVersion=36.0.0\n')
        self.write('gradlew', '#!/bin/sh\nprintf "MOCK_BUILD_REACHED\\n"\nexit 77\n', executable=True)
        for command in ('bash', 'dirname', 'realpath', 'mktemp', 'rsync', 'tail', 'tee', 'mkdir', 'uname'):
            actual = shutil.which(command)
            if actual is None:
                self.skipTest(f'Test harness needs {command}')
            (self.bin / command).symlink_to(actual)
        self.python_file(self.jdk / 'bin/java', 'import os\nprint(\'openjdk version "\' + os.environ.get("VERIFY_TEST_JAVA", "25.0.1") + \'"\')\n')
        self.python_file(self.jdk / 'bin/javac', 'print("javac 25")\n')
        self.tool = self.base / 'sdk-tool'
        self.python_file(self.tool, SDK_TOOL)
        (self.modules / 'apksigcopier.py').write_text('do_copy = zip_data = APKZipInfo = is_meta = None\n')
        # Block all real networking in the subprocess; only explicit fixture URLs are served.
        (self.modules / 'sitecustomize.py').write_text('''import io, os, urllib.request
def fixture_urlopen(url, **kwargs):
    with open(os.environ['VERIFY_TEST_NETWORK'], 'a') as log:
        log.write(str(url) + '\\n')
    if str(url) == 'https://dl.google.com/android/repository/repository2-3.xml':
        path = os.environ['VERIFY_TEST_METADATA']
    elif str(url) == 'https://dl.google.com/android/repository/commandlinetools-linux-99999999_latest.zip':
        path = os.environ['VERIFY_TEST_ARCHIVE']
    else:
        raise OSError('Unexpected network request in isolated test')
    with open(path, 'rb') as source:
        return io.BytesIO(source.read())
urllib.request.urlopen = fixture_urlopen
''')
        self.env = dict(os.environ)
        self.env.update(PATH=str(self.bin), PYTHONPATH=str(self.modules), PYTHONDONTWRITEBYTECODE='1',
                        HOTS_JAVA_HOME=str(self.jdk), HOTS_ANDROID_SDK=str(self.sdk), HOTS_VERIFY_PYTHON=sys.executable,
                        VERIFY_TEST_EVENTS=str(self.events), VERIFY_TEST_NETWORK=str(self.network),
                        VERIFY_TEST_TOOL=str(self.tool), VERIFY_TEST_METADATA=str(self.base / 'repository.xml'),
                        VERIFY_TEST_ARCHIVE=str(self.base / 'tools.zip'))

    def write(self, relative, text, executable=False):
        path = self.project / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        if executable:
            path.chmod(0o755)
        return path

    def python_file(self, path, text):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('#!' + sys.executable + '\n' + text)
        path.chmod(0o755)

    def ready_sdk(self, platform='android-37.0'):
        target = self.sdk / 'platforms' / platform
        target.mkdir(parents=True, exist_ok=True)
        (target / 'android.jar').write_bytes(b'fixture')
        (target / 'source.properties').write_text('Pkg.Revision=1')
        for name in ('aapt2', 'apksigner', 'zipalign'):
            self.python_file(self.sdk / 'build-tools/36.0.0' / name, SDK_TOOL)

    def manager(self, modern=False, nested=False):
        name = 'android' if modern else 'sdkmanager'
        folder = 'cmdline-tools/latest/bin' if nested else 'cmdline-tools/bin'
        self.python_file(self.sdk / folder / name, SDK_MANAGER)

    def download_fixture(self, corrupt=False, unsafe=False):
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as archive:
            archive.writestr('cmdline-tools/bin/sdkmanager', '#!' + sys.executable + '\n' + SDK_MANAGER)
            archive.writestr('cmdline-tools/source.properties', 'Pkg.Revision=99')
            if unsafe:
                archive.writestr('cmdline-tools/../../outside', 'invalid')
        data = output.getvalue()
        Path(self.env['VERIFY_TEST_ARCHIVE']).write_bytes(data)
        Path(self.env['VERIFY_TEST_METADATA']).write_text(metadata(data, checksum='0' * 40 if corrupt else None))

    def run_verifier(self, *flags, input=''):
        return subprocess.run([str(self.script), *flags, str(self.apk)], env=self.env,
                              input=input, text=True, capture_output=True, timeout=30)

    def reaches_mock_build(self, result):
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn('MOCK_BUILD_REACHED', result.stderr)

    def test_generic_ready_sdk_is_read_only_and_has_no_arch_hints(self):
        self.ready_sdk()
        result = self.run_verifier('--check-env')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('CHECK OK', result.stdout)
        self.assertNotIn('pacman', result.stdout + result.stderr)
        self.assertFalse(self.network.exists())
        self.assertFalse(self.events.exists())

    def test_missing_command_has_arch_package_hint_only_when_requested(self):
        (self.bin / 'rsync').unlink()
        for flags in ((), ('--arch-linux',)):
            with self.subTest(flags=flags):
                result = self.run_verifier(*flags)
                self.assertNotEqual(0, result.returncode)
                self.assertIn('Missing commands: rsync', result.stderr)
                self.assertEqual(bool(flags), 'sudo pacman -Syu --needed rsync' in result.stderr)

    def test_missing_python_module_and_wrong_jdk_have_specific_arch_hints(self):
        (self.modules / 'apksigcopier.py').write_text('raise ImportError("fixture")\n')
        result = self.run_verifier('--arch-linux')
        self.assertIn('sudo pacman -Syu --needed python apksigcopier', result.stderr)
        (self.modules / 'apksigcopier.py').write_text('do_copy = zip_data = APKZipInfo = is_meta = None\n')
        self.env['VERIFY_TEST_JAVA'] = '24.0.1'
        result = self.run_verifier('--arch-linux')
        self.assertIn('requires JDK 25', result.stderr)
        self.assertIn('sudo pacman -Syu --needed jdk25-openjdk', result.stderr)
        self.assertFalse(self.network.exists())

    def test_offline_and_check_env_never_bootstrap_or_install(self):
        for flag in ('--offline', '--check-env'):
            with self.subTest(flag=flag):
                result = self.run_verifier('--arch-linux', flag)
                self.assertIn('Automatic SDK setup is disabled', result.stderr)
                self.assertFalse(self.sdk.exists())
                self.assertFalse(self.network.exists())

    def test_legacy_flat_cli_installs_missing_components_and_reuses_them(self):
        self.manager()
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        self.assertTrue((self.sdk / 'platforms/android-37.0/android.jar').is_file())
        calls = self.events.read_text()
        result = self.run_verifier('--arch-linux', '--check-env')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(calls, self.events.read_text())
        self.assertFalse(self.network.exists())

    def test_current_android_cli_uses_its_own_sdk_syntax(self):
        self.manager(modern=True, nested=True)
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        self.assertIn('--ignore-outdated-xmls', self.events.read_text())

    def test_missing_cli_downloads_checks_extracts_and_installs_sdk(self):
        self.download_fixture()
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        self.assertTrue((self.sdk / 'cmdline-tools/latest/bin/sdkmanager').is_file())
        self.assertTrue((self.sdk / 'platforms/android-37.0/android.jar').is_file())
        self.assertEqual(2, len(self.network.read_text().splitlines()))
        self.assertFalse(list((self.sdk / 'cmdline-tools').glob('.hots-cli-*')))

    def test_bad_download_checksum_never_installs_or_runs_the_cli(self):
        self.download_fixture(corrupt=True)
        result = self.run_verifier('--arch-linux', input='y\n')
        self.assertIn('size/checksum check', result.stderr)
        self.assertFalse((self.sdk / 'cmdline-tools/latest').exists())
        self.assertFalse(self.events.exists())
        self.assertFalse(list((self.sdk / 'cmdline-tools').glob('.hots-cli-*')))

    def test_zip_traversal_is_rejected_even_with_a_matching_checksum(self):
        self.download_fixture(unsafe=True)
        result = self.run_verifier('--arch-linux', input='y\n')
        self.assertIn('Unsafe or duplicate path', result.stderr)
        self.assertFalse((self.sdk / 'cmdline-tools/latest').exists())
        self.assertFalse(self.events.exists())

    def test_declining_the_sdk_license_stops_before_building(self):
        self.manager()
        result = self.run_verifier('--arch-linux', input='n\n')
        self.assertIn('Check whether the licenses were accepted', result.stderr)
        self.assertNotIn('MOCK_BUILD_REACHED', result.stderr)

    def test_bare_platform_name_is_supported(self):
        self.manager()
        self.env['VERIFY_TEST_PLATFORM'] = 'android-37'
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        self.assertTrue((self.sdk / 'platforms/android-37/android.jar').is_file())

    def test_versions_come_from_the_project_and_no_other_api_is_substituted(self):
        self.manager()
        self.write('app/build.gradle.kts', 'version = release(38)\napplicationId = "com.example.hots"\nversionCode = 1\n')
        result = self.run_verifier('--arch-linux', input='y\n')
        self.assertIn('No stable SDK Platform API 38', result.stderr)
        self.env['VERIFY_TEST_PLATFORM'] = 'android-38.0'
        self.write('gradle.properties', 'hots.buildToolsVersion=37.0.1\n')
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        self.assertTrue((self.sdk / 'build-tools/37.0.1/aapt2').is_file())

    def test_duplicate_platforms_and_invalid_signature_stop_before_building(self):
        self.ready_sdk()
        self.env['VERIFY_TEST_SIGNATURE_EXIT'] = '1'
        result = self.run_verifier('--arch-linux', '--check-env')
        self.assertIn('reference APK signature does not verify', result.stderr)
        self.ready_sdk(platform='android-37')
        result = self.run_verifier('--arch-linux')
        self.assertIn('Multiple complete SDK Platforms', result.stderr)
        self.assertFalse(self.network.exists())

    def test_only_missing_build_tools_are_installed(self):
        self.ready_sdk()
        (self.sdk / 'build-tools/36.0.0/apksigner').unlink()
        self.manager()
        self.reaches_mock_build(self.run_verifier('--arch-linux', input='y\n'))
        calls = self.events.read_text()
        self.assertNotIn('--list', calls)
        self.assertNotIn('platforms;', calls)


class MetadataTest(unittest.TestCase):
    def test_latest_stable_url_is_read_from_metadata(self):
        result = latest_linux_archive(metadata(b'fixture'))
        self.assertEqual('https://dl.google.com/android/repository/commandlinetools-linux-99999999_latest.zip', result.url)
        self.assertEqual(hashlib.sha1(b'fixture').hexdigest(), result.checksum)

    def test_preview_malformed_and_unexpected_urls_are_rejected(self):
        samples = [metadata(b'x', channel='channel-1'), '<invalid',
                   metadata(b'x').replace('commandlinetools-linux-99999999_latest.zip', 'https://example.com/tools.zip')]
        for sample in samples:
            with self.subTest(sample=sample), self.assertRaises(ValueError):
                latest_linux_archive(sample)


if __name__ == '__main__':
    unittest.main()
