"""Install missing Linux command-line tools using Google's stable SDK index."""

from dataclasses import dataclass
import hashlib
import os
from pathlib import Path, PurePosixPath
import platform
import re
import stat
import sys
import tempfile
from urllib.request import urlopen
import xml.etree.ElementTree as ET
import zipfile

REPOSITORY_URL = "https://dl.google.com/android/repository/"
INDEX_URL = REPOSITORY_URL + "repository2-3.xml"


@dataclass(frozen=True)
class ToolsArchive:
    url: str
    size: int
    algorithm: str
    checksum: str


def latest_linux_archive(metadata):
    try:
        root = ET.fromstring(metadata)
    except ET.ParseError as error:
        raise ValueError("Google's SDK repository response is not valid XML.") from error
    for element in root.iter():
        element.tag = element.tag.rsplit("}", 1)[-1]
    matches = []
    for package in root.findall("remotePackage"):
        if package.get("path") != "cmdline-tools;latest" or package.get("obsolete") == "true":
            continue
        channel = package.find("channelRef")
        if channel is None or channel.get("ref") != "channel-0" or package.find("revision/preview") is not None:
            continue
        for archive in package.findall("archives/archive"):
            if archive.findtext("host-os") != "linux" or archive.findtext("host-arch", "x64") not in ("x64", "x86_64"):
                continue
            name = archive.findtext("complete/url", "").strip()
            checksum = archive.find("complete/checksum")
            size = int(archive.findtext("complete/size", "0"))
            if not re.fullmatch(r"commandlinetools-linux-\d+_latest\.zip", name) or not 0 < size <= 1024**3:
                raise ValueError("Unexpected Linux command-line tools archive in Google's index.")
            if checksum is None or checksum.get("type") not in ("sha1", "sha256", "sha512"):
                raise ValueError("Missing or unsupported archive checksum in Google's index.")
            algorithm = checksum.get("type")
            value = (checksum.text or "").strip().lower()
            if not re.fullmatch(r"[0-9a-f]{%d}" % (hashlib.new(algorithm).digest_size * 2), value):
                raise ValueError("Invalid archive checksum in Google's index.")
            matches.append(ToolsArchive(REPOSITORY_URL + name, size, algorithm, value))
    if len(matches) != 1:
        raise ValueError("Expected one stable Linux cmdline-tools;latest package in Google's index.")
    return matches[0]


def install_command_line_tools(sdk):
    if platform.system() != "Linux" or platform.machine().lower() not in ("x86_64", "amd64"):
        raise ValueError("Automatic command-line tools setup currently supports Linux x86_64 only.")
    sdk = Path(sdk)
    parent = sdk / "cmdline-tools"
    destination = parent / "latest"
    if destination.exists() or destination.is_symlink():
        raise ValueError(f"{destination} already exists but has no usable CLI. Repair it or choose another HOTS_ANDROID_SDK; it will not be overwritten.")
    print(f"Reading Google's stable SDK index: {INDEX_URL}", file=sys.stderr)
    with urlopen(INDEX_URL, timeout=30) as response:
        metadata = response.read(8 * 1024**2 + 1)
    if len(metadata) > 8 * 1024**2:
        raise ValueError("SDK repository index is unexpectedly large.")
    archive = latest_linux_archive(metadata)
    parent.mkdir(parents=True, exist_ok=True)
    # Stage next to the destination: interrupted downloads cannot leave a usable-looking installation.
    with tempfile.TemporaryDirectory(prefix=".hots-cli-", dir=parent) as temporary:
        temporary = Path(temporary)
        zipped = temporary / "tools.zip"
        digest = hashlib.new(archive.algorithm)
        received = 0
        reported = 0
        print(f"Downloading {archive.url} ({archive.size / 1024**2:.1f} MiB)", file=sys.stderr)
        with urlopen(archive.url, timeout=60) as response, zipped.open("wb") as output:
            while chunk := response.read(1024**2):
                received += len(chunk)
                if received > archive.size:
                    raise ValueError("Downloaded archive exceeds the size published by Google.")
                digest.update(chunk)
                output.write(chunk)
                if received - reported >= 16 * 1024**2:
                    print(f"Downloaded {received / 1024**2:.0f}/{archive.size / 1024**2:.0f} MiB", file=sys.stderr)
                    reported = received
        if received != archive.size or digest.hexdigest() != archive.checksum:
            raise ValueError("Command-line tools download failed its size/checksum check; nothing was installed.")
        extracted = temporary / "extracted"
        with zipfile.ZipFile(zipped) as source:
            entries = source.infolist()
            if sum(entry.file_size for entry in entries) > 2 * 1024**3:
                raise ValueError("Unpacked command-line tools archive is unexpectedly large.")
            seen = set()
            for entry in entries:
                path = PurePosixPath(entry.filename)
                mode = entry.external_attr >> 16
                if (not path.parts or path.parts[0] != "cmdline-tools" or path.is_absolute()
                        or ".." in path.parts or "\\" in entry.filename or path in seen
                        or stat.S_IFMT(mode) not in (0, stat.S_IFREG, stat.S_IFDIR)):
                    raise ValueError("Unsafe or duplicate path in command-line tools archive.")
                seen.add(path)
            for entry in entries:
                target = Path(source.extract(entry, extracted))
                target.chmod(0o755 if entry.is_dir() or (entry.external_attr >> 16) & 0o111 else 0o644)
        unpacked = extracted / "cmdline-tools"
        name = next((name for name in ("sdkmanager", "android") if (unpacked / "bin" / name).is_file()), None)
        if name is None or not (unpacked / "source.properties").is_file():
            raise ValueError("Downloaded archive has no complete command-line tools installation.")
        (unpacked / "bin" / name).chmod(0o755)
        if destination.exists() or destination.is_symlink():
            raise ValueError("Command-line tools destination changed during setup; refusing to overwrite it.")
        os.rename(unpacked, destination)
    print(f"Installed command-line tools in {destination}", file=sys.stderr)
    return destination / "bin" / name
