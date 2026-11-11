#!/usr/bin/env python3
"""Build a read-only OTA package to dump DaLauncher.apk/.odex, frameworks, and build.prop files.

Designed for units without full raw dumps yet (or non-0040 regions like 1115 / AU):
mounts APP (/system) and CAP (/system/vendor) strictly read-only (-o ro), copies
only the files required by deodex-apk.sh and rebuild-dalauncher.sh (~30 MB),
writes DUMP-APKS.OK / DUMP-APKS.FAIL, and breaks the daupdater loop before sleeping.
"""
from __future__ import annotations

import argparse
import logging
import shutil
import subprocess
import sys
from datetime import UTC, datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent.parent
INPUT_DIR = PROJECT / "tooling" / "references" / "ic1101" / "ota-builder" / "input"
UPSTREAM_SRC = PROJECT / "tooling" / "references" / "ic1101" / "ota-builder" / "src"
sys.path.insert(0, str(UPSTREAM_SRC))
sys.path.insert(0, str(HERE))

import main as upstream  # noqa: E402
import ota_common  # noqa: E402

logger = logging.getLogger("build-dump-apks")
BUSYBOX_SRC = HERE / "bin" / "busybox"
APP_DEV = "/dev/block/platform/sdhci-tegra.3/by-name/APP"
CAP_DEV = "/dev/block/platform/sdhci-tegra.3/by-name/CAP"


def _patched_stage_tree(
    staging, overlay_paths, meta, rom_type, overlay_dir, update_binary, sleep_binary, partitions
):
    """Stage minimal placeholder props, busybox, sleep, and our read-only dump updater-script."""
    (staging / "system" / "vendor").mkdir(parents=True, exist_ok=True)
    upstream.write_lf(
        staging / "system" / "build.prop",
        f"ro.build.id={upstream.NEW_DA_VERSION}\nro.build.tags={upstream.NEW_DA_BUILD_TAGS}\n",
    )
    upstream.write_lf(
        staging / "system" / "vendor" / "build.prop",
        f"custom_rom.type={rom_type}\n",
    )

    if not sleep_binary.is_file():
        raise upstream.BuildError(f"missing sleep binary: {sleep_binary}")
    sleep_dst = staging / upstream.SLEEP_PKG_PATH
    sleep_dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(sleep_binary, sleep_dst)

    if not BUSYBOX_SRC.is_file():
        raise upstream.BuildError(f"missing busybox binary: {BUSYBOX_SRC}")
    shutil.copy2(BUSYBOX_SRC, staging / "busybox")

    meta_dir = staging / "META-INF" / "com" / "google" / "android"
    meta_dir.mkdir(parents=True, exist_ok=True)
    if not update_binary.is_file():
        raise upstream.BuildError(f"missing {update_binary}")
    shutil.copy2(update_binary, meta_dir / "update-binary")
    (meta_dir / "updater-script").write_text(_updater_script(rom_type))


def _updater_script(rom_type: str) -> str:
    """Generate edify updater-script that dumps APKs and frameworks read-only onto USB."""
    BB = "/tmp/busybox"
    USB = "/mnt/usbdrive1"
    OUT = f"{USB}/dump-{rom_type}"
    LOG = f"{OUT}/dump.log"

    def sh(cmd: str) -> str:
        escaped = cmd.replace("\\", "\\\\").replace('"', '\\"')
        return f'run_program("{BB}", "ash", "-c", "{escaped}");'

    return "\n".join([
        "show_progress(1.000000, 0);",
        "set_progress(0.000000);",
        'package_extract_file("busybox", "/tmp/busybox");',
        'set_perm(0, 0, 0755, "/tmp/busybox");',
        "set_progress(0.100000);",
        sh(
            f"{BB} rm -f {USB}/DUMP-APKS.OK {USB}/DUMP-APKS.FAIL; "
            f"{BB} mkdir -p {OUT}/vendor-app {OUT}/system-framework /tmp/sys /tmp/ven; "
            f"{BB} echo '=== DUMP-APKS rom_type={rom_type} ===' > {LOG}"
        ),
        sh(
            f"{BB} umount /tmp/ven 2>/dev/null || true; "
            f"{BB} umount /tmp/sys 2>/dev/null || true; "
            f"{BB} mount -t ext4 -o ro {APP_DEV} /tmp/sys >>{LOG} 2>&1; "
            f"{BB} echo mount-app-exit=$? >> {LOG}; "
            f"{BB} mount -t ext4 -o ro {CAP_DEV} /tmp/ven >>{LOG} 2>&1; "
            f"{BB} echo mount-cap-exit=$? >> {LOG}"
        ),
        "set_progress(0.300000);",
        sh(
            f"{BB} cp /tmp/sys/build.prop {OUT}/system-build.prop >>{LOG} 2>&1; "
            f"{BB} cp /tmp/ven/build.prop {OUT}/vendor-build.prop >>{LOG} 2>&1; "
            f"{BB} cp /tmp/ven/app/DaLauncher.apk {OUT}/vendor-app/DaLauncher.apk >>{LOG} 2>&1; "
            f"{BB} cp /tmp/ven/app/DaLauncher.odex {OUT}/vendor-app/DaLauncher.odex >>{LOG} 2>&1; "
            f"{BB} cp -r /tmp/sys/framework/. {OUT}/system-framework/ >>{LOG} 2>&1; "
            f"if [ -d /tmp/ven/framework ]; then "
            f"{BB} mkdir -p {OUT}/vendor-framework; "
            f"{BB} cp -r /tmp/ven/framework/. {OUT}/vendor-framework/ >>{LOG} 2>&1; "
            f"fi"
        ),
        "set_progress(0.650000);",
        sh(
            f"{BB} ls -la {OUT} {OUT}/vendor-app >>{LOG} 2>&1; "
            f"{BB} md5sum {OUT}/system-build.prop {OUT}/vendor-build.prop "
            f"{OUT}/vendor-app/DaLauncher.apk {OUT}/vendor-app/DaLauncher.odex > {OUT}/hashes.md5 2>>{LOG}; "
            f"{BB} umount /tmp/ven >>{LOG} 2>&1 || true; "
            f"{BB} umount /tmp/sys >>{LOG} 2>&1 || true"
        ),
        sh(
            f"if [ -s {OUT}/system-build.prop ] && [ -s {OUT}/vendor-build.prop ] && "
            f"[ -s {OUT}/vendor-app/DaLauncher.apk ] && [ -s {OUT}/vendor-app/DaLauncher.odex ] && "
            f"{BB} find {OUT}/system-framework -maxdepth 1 -type f | {BB} grep -q .; then "
            f"{BB} echo ok > {USB}/DUMP-APKS.OK; {BB} echo GATE=OK >> {LOG}; "
            f"else "
            f"{BB} echo fail > {USB}/DUMP-APKS.FAIL; {BB} echo GATE=FAIL >> {LOG}; "
            f"fi; "
            f"{BB} sync"
        ),
        "set_progress(0.900000);",
        sh(
            f"{BB} mv {USB}/SwUpdate2.txt {USB}/SwUpdate2.txt.done 2>>{LOG}; "
            f"{BB} echo mv-trigger-exit=$? >> {LOG}; "
            f"{BB} rm -f /cache/.copy_complete 2>>{LOG}; "
            f"{BB} sync"
        ),
        f'package_extract_file("{upstream.SLEEP_PKG_PATH}", "/tmp/sleep");',
        'set_perm(0, 0, 0755, "/tmp/sleep");',
        "set_progress(1.000000);",
        'run_program("/tmp/sleep", "30");',
    ]) + "\n"


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    ap = argparse.ArgumentParser(
        description="Build OTA to dump DaLauncher.apk/.odex, frameworks, and build.prop files read-only."
    )
    ota_common.add_rom_type_arg(ap)
    ap.add_argument(
        "--output-dir",
        type=Path,
        default=HERE / "output-dump-apks",
        help="Output directory for generated build tree",
    )
    args = ap.parse_args()
    rom_type = args.rom_type or ota_common.default_rom_type()

    upstream.stage_tree = _patched_stage_tree
    try:
        build_dir = args.output_dir / datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
        if build_dir.exists():
            raise upstream.BuildError(f"build dir already exists: {build_dir}")
        build_dir.mkdir(parents=True)
        upstream.write_lf(build_dir / "SwUpdate2.txt", f"Ver:{upstream.NEW_COMMAND_VERSION}\n")
        out = upstream.build_mdt(
            rom_type,
            [],
            {},
            build_dir,
            overlay_dir=HERE,
            update_binary=INPUT_DIR / "update-binary",
            sleep_binary=INPUT_DIR / "sleep" / "output" / "sleep",
            signapk_jar=INPUT_DIR / "signapk.jar",
            sign_cert=INPUT_DIR / "keys" / "testkey.x509.pem",
            sign_key=INPUT_DIR / "keys" / "testkey.pk8",
            partitions=upstream.PARTITIONS,
        )
        logger.info("DUMP-APKS OTA ready: %s (custom_rom.type=%s)", out, rom_type)
    except (upstream.BuildError, OSError, subprocess.CalledProcessError):
        logger.exception("build failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
