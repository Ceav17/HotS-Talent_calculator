#!/usr/bin/env python3
"""Verify a rebuilt APK against a signed APK, including modern signer ZIP alignment.

Requires apksigcopier. No private key is read and no APK is signed.
References:
https://android.googlesource.com/platform/tools/apksig/+/master/src/apksigner/java/com/android/apksigner/help_sign.txt
https://github.com/obfusk/apksigcopier
https://github.com/python/cpython/blob/3.14/Lib/zipfile/__init__.py
"""

import argparse
import filecmp
import hashlib
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
import zipfile

import apksigcopier


def copy_bytes(source, destination, size):
    while size:
        chunk = source.read(min(size, 1024 * 1024))
        if not chunk:
            raise ValueError("Truncated ZIP input")
        destination.write(chunk)
        size -= len(chunk)


def retained_extra(extra):
    retained = bytearray()
    while len(extra) >= 4:
        kind, size = struct.unpack_from("<HH", extra)
        end = size + 4
        if end > len(extra):
            raise ValueError("Malformed ZIP extra field")
        if (kind, size) != (0, 0) and kind != 0xD935:
            retained.extend(extra[:end])
        extra = extra[end:]
    if any(extra):
        raise ValueError("Unsupported trailing ZIP extra data")
    return bytes(retained)


def signer_alignment(source_path, output_path):
    """Apply apksigner's default padding to the rebuilt bytes, without recompression."""
    with zipfile.ZipFile(source_path) as archive:
        entries = archive.infolist()
        if len({item.filename for item in entries}) != len(entries):
            raise ValueError("Duplicate ZIP names are unsupported")
        if any(apksigcopier.is_meta(item.filename) for item in entries):
            raise ValueError("The rebuilt input must be unsigned")
    data = apksigcopier.zip_data(str(source_path))
    if len(entries) >= 65535 or source_path.stat().st_size >= 0xFFFFFFFF:
        raise ValueError("ZIP64 archives are unsupported")
    offsets = {}
    with source_path.open("rb") as source, output_path.open("wb") as output:
        for entry in sorted(entries, key=lambda item: item.header_offset):
            gap = entry.header_offset - source.tell()
            if gap < 0:
                raise ValueError("Overlapping ZIP entries")
            copy_bytes(source, output, gap)
            header = bytearray(source.read(30))
            if header[:4] != b"PK\x03\x04":
                raise ValueError("Invalid local ZIP header")
            name_size, extra_size = struct.unpack_from("<HH", header, 26)
            name, extra = source.read(name_size), source.read(extra_size)
            offsets[entry.filename] = output.tell()
            if entry.compress_type == zipfile.ZIP_STORED:
                extra = retained_extra(extra)
                alignment = 16384 if entry.filename.endswith(".so") else 4
                padding = -(output.tell() + 30 + len(name) + len(extra) + 6) % alignment
                extra += struct.pack("<HHH", 0xD935, padding + 2, alignment)
                extra += bytes(padding)
                struct.pack_into("<H", header, 28, len(extra))
            output.write(header)
            output.write(name)
            output.write(extra)
            copy_bytes(source, output, entry.compress_size)

        # Preserve any descriptors or gaps; only padding and offsets are changed.
        gap = data.cd_offset - source.tell()
        if gap < 0:
            raise ValueError("Invalid central-directory offset")
        copy_bytes(source, output, gap)
        new_directory = output.tell()
        directory = bytearray(data.cd_and_eocd)
        cursor = 0
        for entry in entries:
            if directory[cursor:cursor + 4] != b"PK\x01\x02":
                raise ValueError("Invalid central-directory record")
            name_size, extra_size, comment_size = struct.unpack_from("<HHH", directory, cursor + 28)
            struct.pack_into("<I", directory, cursor + 42, offsets[entry.filename])
            cursor += 46 + name_size + extra_size + comment_size
        end = data.eocd_offset - data.cd_offset
        if cursor != end or directory[end:end + 4] != b"PK\x05\x06":
            raise ValueError("Unsupported central-directory layout")
        struct.pack_into("<I", directory, end + 16, new_directory)
        output.write(directory)


def preserve_filename_flags(info):
    # Python 3.14 strips UTF-8 flags from ASCII names; APK signatures cover them.
    if info.flag_bits & 0x800:
        return info.filename.encode("utf-8"), info.flag_bits
    try:
        return info.filename.encode("cp437"), info.flag_bits
    except UnicodeEncodeError:
        return info.filename.encode("utf-8"), info.flag_bits | 0x800


def digest(path):
    result = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            result.update(chunk)
    return result.hexdigest()


def check_payloads(signed, unsigned):
    """Reject actual application differences before considering ZIP metadata."""
    def entries(archive):
        items = archive.infolist()
        if len({item.filename for item in items}) != len(items):
            raise ValueError("Duplicate ZIP entry names are unsupported")
        return {item.filename: item for item in items if not apksigcopier.is_meta(item.filename)}

    with zipfile.ZipFile(signed) as a, zipfile.ZipFile(unsigned) as b:
        first, second = entries(a), entries(b)
        different = sorted(first.keys() ^ second.keys())
        for name in sorted(first.keys() & second.keys()):
            with a.open(name) as x, b.open(name) as y:
                while True:
                    left, right = x.read(1024 * 1024), y.read(1024 * 1024)
                    if left != right:
                        different.append(name)
                        break
                    if not left:
                        break
        if different:
            names = "\n".join("  " + name for name in different[:25])
            raise ValueError(f"Rebuilt application contents differ ({len(different)} files):\n{names}\nCheck the release sources and clean-build/toolchain settings.")


def local_extra(stream, entry):
    stream.seek(entry.header_offset)
    header = stream.read(30)
    if header[:4] != b"PK\x03\x04":
        raise ValueError("Invalid local ZIP header")
    name_size, extra_size = struct.unpack_from("<HH", header, 26)
    stream.seek(name_size, 1)
    return stream.read(extra_size)


def needs_signer_alignment(signed, unsigned):
    """Detect the signer's padding transformation from ZIP headers, not payloads."""
    with zipfile.ZipFile(signed) as a, zipfile.ZipFile(unsigned) as b, signed.open("rb") as x, unsigned.open("rb") as y:
        second = {item.filename: item for item in b.infolist()}
        for entry in a.infolist():
            other = second.get(entry.filename)
            if other is None or entry.compress_type != zipfile.ZIP_STORED:
                continue
            first_extra, second_extra = local_extra(x, entry), local_extra(y, other)
            if first_extra == second_extra:
                continue
            cursor = 0
            while cursor + 4 <= len(first_extra):
                kind, size = struct.unpack_from("<HH", first_extra, cursor)
                if cursor + size + 4 > len(first_extra):
                    break
                if kind == 0xD935:
                    return True
                cursor += size + 4
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("signed", type=Path)
    parser.add_argument("unsigned", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--apksigner", default=str(Path.home() / "Android/Sdk/build-tools/36.0.0/apksigner"))
    parser.add_argument("--java-home", default="/opt/android-studio/jbr")
    parser.add_argument("--signer-alignment", action="store_true", help="Reproduce apksigner's default 4-byte/16-KiB ZIP padding")
    parser.add_argument("--alignment-mode", choices=("auto", "preserve", "signer"), default="auto")
    args = parser.parse_args()
    if args.output.exists() or args.output.is_symlink():
        parser.error("Output already exists; choose a new filename")
    environment = dict(os.environ, JAVA_HOME=args.java_home)
    subprocess.run([args.apksigner, "verify", str(args.signed)], env=environment, check=True)
    check_payloads(args.signed, args.unsigned)
    normalize = args.signer_alignment or args.alignment_mode == "signer"
    if not normalize and args.alignment_mode == "auto":
        normalize = needs_signer_alignment(args.signed, args.unsigned)
    print("ZIP alignment mode: " + ("signer" if normalize else "preserve"), flush=True)

    # This override applies only to this process, not to installed packages.
    apksigcopier.APKZipInfo._encodeFilenameFlags = preserve_filename_flags
    with tempfile.TemporaryDirectory(prefix=".apk-verify-", dir=args.output.parent) as temporary:
        temporary = Path(temporary)
        unsigned = args.unsigned
        if normalize:
            unsigned = temporary / "aligned-unsigned.apk"
            signer_alignment(args.unsigned, unsigned)
        reconstructed = temporary / "reproduced.apk"
        apksigcopier.do_copy(str(args.signed), str(unsigned), str(reconstructed))
        subprocess.run([args.apksigner, "verify", "--verbose", str(reconstructed)], env=environment, check=True)
        if not filecmp.cmp(args.signed, reconstructed, shallow=False):
            raise SystemExit("DIFFERENT: signature verifies but APK bytes differ")
        # Publish only a verified result, without overwriting any existing file.
        with args.output.open("xb") as destination, reconstructed.open("rb") as source:
            shutil.copyfileobj(source, destination)
    print(f"PASS: valid signature and byte-for-byte identical APKs.\nSHA-256: {digest(args.output)}\nOutput: {args.output}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, zipfile.BadZipFile, apksigcopier.APKSigCopierError, subprocess.CalledProcessError) as error:
        raise SystemExit("ERROR: " + str(error)) from None
