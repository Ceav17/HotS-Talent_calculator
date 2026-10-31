#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'HELP'
Usage: ./verify-reproducibility.sh [OPTIONS] SIGNED_APK

Rebuild this project from a fresh source copy, transfer the public signature,
and require a valid signature AND byte-for-byte identical APKs. No private key.

Options:
  --offline          Do not download Gradle dependencies.
  --check-env        Check prerequisites and the signed APK, without building.
  --project DIR      Source project (default: directory containing this script).
  --work-dir DIR     Parent for a new results directory (default: PROJECT/build/reproducibility).
  -h, --help         Show this help.

Environment:
  HOTS_JAVA_HOME      JDK directory. Otherwise prefer Android Studio's JBR,
                     then JAVA_HOME, then java on PATH. This project needs JDK 25.
  HOTS_ANDROID_SDK    SDK directory. Otherwise use ANDROID_HOME, ANDROID_SDK_ROOT,
                     local.properties, then ~/Android/Sdk.
  HOTS_VERIFY_PYTHON  Python executable with the apksigcopier module (default: python3).

Linux requirements:
  Commands: bash (4+), dirname, realpath, mktemp, rsync, tail, tee, mkdir, uname.
  Python: python3 (3.9+) with the apksigcopier module, or HOTS_VERIFY_PYTHON.
  JDK 25: java and javac.
  Android SDK: the project's Platform and Build Tools, including apksigner,
               aapt2 and zipalign. Use the Gradle wrapper included in the project.
GNU-compatible command options are required. Make the listed commands available
on PATH or configure the JDK, SDK and Python paths using the variables above.
The script does not install software or change versions automatically.
HELP
}

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
if (( BASH_VERSINFO[0] < 4 )); then die 'Bash 4 or newer is required.'; fi

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
project=$script_dir
work_parent=''
signed=''
offline=false
check_env=false
while (( $# )); do
    case "$1" in
        --offline) offline=true ;;
        --check-env) check_env=true ;;
        --project|--work-dir)
            (( $# >= 2 )) && [[ -n $2 ]] || die "Missing value for $1"
            if [[ $1 == --project ]]; then project=$2; else work_parent=$2; fi
            shift ;;
        -h|--help) usage; exit 0 ;;
        --)
            shift
            (( $# == 1 )) && [[ -z $signed ]] || die 'Expected one signed APK.'
            signed=$1; shift; break ;;
        -*) die "Unknown option: $1" ;;
        *) [[ -z $signed ]] || die 'Expected only one signed APK.'; signed=$1 ;;
    esac
    shift
done
[[ -n $signed ]] || { usage >&2; exit 2; }

python=${HOTS_VERIFY_PYTHON:-python3}
missing=()
for program in realpath mktemp rsync tail tee mkdir uname "$python"; do
    command -v "$program" >/dev/null 2>&1 || missing+=("$program")
done
if (( ${#missing[@]} )); then
    printf 'Missing commands: %s\n' "${missing[*]}" >&2
    printf 'Make the commands listed above available on PATH, then rerun this script.\n' >&2
    exit 1
fi
[[ $(uname -s) == Linux ]] || die 'This script currently supports Linux only.'
project=$(realpath -e -- "$project")
signed=$(realpath -e -- "$signed")
[[ -f $signed && -r $signed ]] || die 'The signed APK must be a readable file.'
helper="$script_dir/tools/verify_apk_reproducibility.py"
[[ -f $helper ]] || die "Missing bundled helper: $helper"
for relative in gradlew gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties settings.gradle.kts app/build.gradle.kts gradle.properties; do
    [[ -f $project/$relative ]] || die "Missing project file: $relative"
done
[[ -x $project/gradlew ]] || die 'gradlew is not executable; run chmod +x gradlew.'

if ! "$python" -c 'import sys; assert sys.version_info >= (3, 9); import apksigcopier; assert all(hasattr(apksigcopier, x) for x in ("do_copy", "zip_data", "APKZipInfo", "is_meta"))'; then
    printf 'Python 3.9+ with a compatible apksigcopier module is required.\n' >&2
    printf 'The apksigcopier module must be importable by the selected Python interpreter.\n' >&2
    printf 'Set HOTS_VERIFY_PYTHON to that Python executable if it is not python3.\n' >&2
    exit 1
fi

if [[ -n ${HOTS_JAVA_HOME:-} ]]; then
    jdk=$HOTS_JAVA_HOME
elif [[ -x /opt/android-studio/jbr/bin/java ]]; then
    jdk=/opt/android-studio/jbr
elif [[ -n ${JAVA_HOME:-} ]]; then
    jdk=$JAVA_HOME
elif command -v java >/dev/null 2>&1; then
    jdk=$(realpath -e -- "$(command -v java)"); jdk=${jdk%/bin/java}
else
    die 'JDK not found. Set HOTS_JAVA_HOME to a JDK 25 installation.'
fi
jdk=$(realpath -e -- "$jdk")
[[ -x $jdk/bin/java && -x $jdk/bin/javac ]] || die "Not a complete JDK: $jdk"
export JAVA_HOME=$jdk

# Read project settings and reject wrong SDKs, JDKs and APK identities before building.
config=$("$python" - "$project" "$signed" "$jdk" <<'PY'
from pathlib import Path
import os, re, subprocess, sys

project, signed, jdk = map(Path, sys.argv[1:])
def fail(message):
    raise SystemExit('ERROR: ' + message)
def one(pattern, text, label):
    values = re.findall(pattern, text, re.M)
    if len(values) != 1:
        fail('Cannot determine ' + label + ' from this project.')
    return values[0]

config = (project / 'app/build.gradle.kts').read_text()
api = one(r'version\s*=\s*release\((\d+)\)', config, 'compileSdk')
app_id = one(r'^\s*applicationId\s*=\s*"([^"\n]+)"', config, 'applicationId')
version_code = one(r'^\s*versionCode\s*=\s*(\d+)\b', config, 'versionCode')
tools_version = one(r'^hots\.buildToolsVersion\s*=\s*(\d+\.\d+\.\d+)\s*$', (project / 'gradle.properties').read_text(), 'Build Tools version')
java = subprocess.run([str(jdk / 'bin/java'), '-version'], text=True, capture_output=True, check=True)
java_output = java.stderr + java.stdout
match = re.search(r'version\s+"?(\d+)', java_output)
if not match or match[1] != '25':
    fail('This project requires JDK 25. Set HOTS_JAVA_HOME to the correct JDK.')

sdk = os.environ.get('HOTS_ANDROID_SDK') or os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
local = project / 'local.properties'
if not sdk and local.is_file():
    match = re.search(r'^sdk\.dir\s*=\s*(.+)$', local.read_text(), re.M)
    if match:
        sdk = re.sub(r'\\([\\ :=])', r'\1', match[1].strip())
sdk = Path(sdk).expanduser().resolve() if sdk else Path.home() / 'Android/Sdk'
tools = sdk / 'build-tools' / tools_version
for name in ('apksigner', 'aapt2', 'zipalign'):
    if not os.access(tools / name, os.X_OK):
        fail(f'Missing executable: {tools / name}. Provide Android SDK Build Tools {tools_version}, or set HOTS_ANDROID_SDK to the correct SDK directory.')
platforms = [sdk / 'platforms' / f'android-{suffix}' for suffix in (api, api + '.0')]
platforms = [p for p in platforms if (p / 'android.jar').is_file() and (p / 'source.properties').is_file()]
if len(platforms) != 1:
    fail(f'Expected exactly one complete SDK Platform android-{api} or android-{api}.0 in {sdk}/platforms; found {len(platforms)}. Check the SDK Platform files or HOTS_ANDROID_SDK.')
badging = subprocess.run([str(tools / 'aapt2'), 'dump', 'badging', str(signed)], text=True, capture_output=True)
if badging.returncode:
    fail('aapt2 could not read the APK. Check the APK and Linux SDK installation.\n' + badging.stderr[:1500])
match = re.search(r"^package: name='([^']+)' versionCode='(\d+)'", badging.stdout, re.M)
if not match or match[1] != app_id or match[2] != version_code:
    fail(f'APK applicationId/versionCode do not match these sources ({app_id}, {version_code}). Check out the corresponding release sources; no build started.')
for value in (str(sdk), str(tools), tools_version, str(platforms[0]), api, java_output.splitlines()[0]):
    if '\n' in value or '\r' in value:
        fail('Newlines in configuration paths are unsupported.')
    print(value)
PY
) || exit 1
mapfile -t config_values <<< "$config"
sdk=${config_values[0]}
build_tools=${config_values[1]}
platform=${config_values[3]}
export ANDROID_HOME=$sdk ANDROID_SDK_ROOT=$sdk

if ! signature_report=$("$build_tools/apksigner" verify --verbose --print-certs "$signed" 2>&1); then
    printf '%s\n' "$signature_report" >&2
    die 'The reference APK signature does not verify. No build started.'
fi
printf 'Prerequisites OK: %s; API %s; Build Tools %s.\n' "${config_values[5]}" "${config_values[4]}" "${config_values[2]}"
if "$check_env"; then
    printf 'CHECK OK: environment and reference APK signature checked; no build performed.\n'
    exit 0
fi

work_parent=${work_parent:-$project/build/reproducibility}
work_parent=$(realpath -m -- "$work_parent")
if [[ $work_parent == "$project" || $work_parent == "$project/"* ]]; then
    [[ $work_parent == "$project/build" || $work_parent == "$project/build/"* ]] || die 'Inside the source project, --work-dir must be under its build directory.'
fi
mkdir -p -- "$work_parent"
work=$(mktemp -d "$work_parent/verify.XXXXXXXX")
printf 'Results directory: %s\n' "$work"
printf '%s\n' "$signature_report" > "$work/reference-signature.txt"
printf '%s\n' "$config" > "$work/environment.txt"
trap 'printf "Interrupted. Logs retained in: %s\n" "$work" >&2; exit 130' INT
trap 'printf "Terminated. Logs retained in: %s\n" "$work" >&2; exit 143' TERM

rsync -a --exclude='.git/' --exclude='.gradle/' --exclude='.kotlin/' \
    --exclude='.idea/' --exclude='build/' --exclude='.cxx/' \
    --exclude='*.p12' --exclude='*.pfx' --exclude='*.jks' --exclude='*.keystore' \
    --exclude='*.key' --exclude='*.pk8' --exclude='keystore.properties' --exclude='signing.properties' \
    "$project/" "$work/source/"
# Force Gradle to use the SDK checked above, changing only the source copy.
"$python" - "$work/source/local.properties" "$sdk" <<'PY'
from pathlib import Path
import re, sys
path = Path(sys.argv[1])
original = path.read_text() if path.is_file() else ''
original = re.sub(r'^sdk\.dir\s*=.*(?:\n|$)', '', original, flags=re.M)
value = sys.argv[2].replace('\\', '\\\\').replace(' ', '\\ ').replace(':', '\\:').replace('=', '\\=')
path.write_text(original.rstrip('\n') + '\nsdk.dir=' + value + '\n')
PY

gradle_options=(--no-daemon --no-build-cache --no-configuration-cache --console=plain --show-version
    -Porg.gradle.java.installations.auto-detect=false
    -Porg.gradle.java.installations.auto-download=false
    "-Porg.gradle.java.installations.paths=$jdk")
if "$offline"; then gradle_options+=(--offline); fi
printf 'Building one fresh unsigned release. Log: %s/build.log\n' "$work"
if (cd "$work/source" && ./gradlew "${gradle_options[@]}" :app:assembleRelease) > "$work/build.log" 2>&1; then
    :
else
    tail -n 60 "$work/build.log" >&2
    die "Build failed. Full log: $work/build.log"
fi
unsigned="$work/source/app/build/outputs/apk/release/app-release-unsigned.apk"
[[ -f $unsigned ]] || die "Build completed but unsigned APK is missing: $unsigned"
if ! "$build_tools/zipalign" -c -P 16 4 "$unsigned" > "$work/alignment.txt" 2>&1; then
    tail -n 20 "$work/alignment.txt" >&2
    die 'The rebuilt APK is not aligned as required.'
fi
if "$python" "$helper" --alignment-mode auto --apksigner "$build_tools/apksigner" --java-home "$jdk" \
    "$signed" "$unsigned" "$work/reproduced.apk" > "$work/verification.txt" 2>&1; then
    :
else
    tail -n 60 "$work/verification.txt" >&2
    die "Reproducibility verification failed. Sources and logs retained in: $work"
fi

"$python" - "$project" "$signed" "$unsigned" "$work" "$platform" <<'PY'
from pathlib import Path
import hashlib, json, shutil, subprocess, sys
project, signed, unsigned, work, platform = map(Path, sys.argv[1:])
def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            result.update(chunk)
    return result.hexdigest()
commit, clean = None, None
if shutil.which('git') and (project / '.git').exists():
    head = subprocess.run(['git', '-C', str(project), 'rev-parse', 'HEAD'], capture_output=True, text=True)
    status = subprocess.run(['git', '-C', str(project), 'status', '--porcelain'], capture_output=True, text=True)
    if head.returncode == 0: commit = head.stdout.strip()
    if status.returncode == 0: clean = not status.stdout.strip()
report = {'result': 'PASS', 'signed_apk': str(signed), 'source_project': str(project),
          'source_commit': commit, 'source_clean': clean, 'signed_apk_sha256': digest(signed),
          'rebuilt_unsigned_sha256': digest(unsigned), 'reproduced_sha256': digest(work / 'reproduced.apk'),
          'android_jar_sha256': digest(platform / 'android.jar'),
          'environment': (work / 'environment.txt').read_text().splitlines()}
(work / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
(work / 'SHA256SUMS').write_text(report['reproduced_sha256'] + '  reproduced.apk\n')
print('PASS: fresh rebuild, valid signature, and byte-for-byte identical signed APKs.')
print('SHA-256: ' + report['reproduced_sha256'])
print('Report: ' + str(work / 'report.json'))
PY
