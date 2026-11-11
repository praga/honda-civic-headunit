#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import logging
import shutil
import sys
from datetime import UTC, datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent.parent
OTA_BUILDER_SRC = PROJECT / "tooling" / "references" / "ic1101" / "ota-builder" / "src"
INPUT_DIR = PROJECT / "tooling" / "references" / "ic1101" / "ota-builder" / "input"
OUT_DIR = HERE / "output-dalauncher-restore"

DALAUNCHER_APK_SRC = PROJECT / "tooling" / "apks-dumped" / "dump-0040" / "vendor-app" / "DaLauncher.apk"
DALAUNCHER_ODEX_SRC = PROJECT / "tooling" / "apks-dumped" / "dump-0040" / "vendor-app" / "DaLauncher.odex"

EXPECTED_APK_SHA256 = "1d8bd729ad309240bc77c6c2d5f9a93c07940cc355d4adbed9930ee6d9cee019"
EXPECTED_ODEX_SHA256 = "1497dc8497799faf8e86fe8f90fede54dd98502472f1c3e03b82b4542eb57db7"
EXPECTED_APK_MD5 = "20e0cba9e8b0d34d025cc1e1cb88992f"
EXPECTED_ODEX_MD5 = "7b5842ef9980c9221655911b2d3e1d1e"

for _p in (HERE, OTA_BUILDER_SRC):
    if str(_p) not in sys.path:
        sys.path.insert(0, str(_p))
import main as upstream
from main import FileMeta
import ota_common

BUSYBOX_SRC = HERE / "bin" / "busybox"
ROM_TYPE = ota_common.default_rom_type()

OVERLAY_PATHS = [
    "system/vendor/app/DaLauncher.apk",
    "system/vendor/app/DaLauncher.odex",
]

OVERLAY_META = {
    "system/vendor/app/DaLauncher.apk": FileMeta(0, 0, 0o644),
    "system/vendor/app/DaLauncher.odex": FileMeta(0, 0, 0o644),
}


def _digest(path: Path, algo: str) -> str:
    h = hashlib.new(algo)
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def _source_paths(rom_type: str) -> tuple[Path, Path]:
    if rom_type == "0040":
        return DALAUNCHER_APK_SRC, DALAUNCHER_ODEX_SRC
    vendor_app = PROJECT / "tooling" / "apks-dumped" / f"dump-{rom_type}" / "vendor-app"
    return vendor_app / "DaLauncher.apk", vendor_app / "DaLauncher.odex"


def _verify_source(rom_type: str = "0040") -> tuple[str, str]:
    apk_src, odex_src = _source_paths(rom_type)
    for src in (apk_src, odex_src):
        if not src.is_file():
            raise SystemExit(f"missing source: {src}")
    if rom_type == "0040":
        for src, want in [(apk_src, EXPECTED_APK_SHA256), (odex_src, EXPECTED_ODEX_SHA256)]:
            got = _digest(src, "sha256")
            if got != want:
                raise SystemExit(f"sha256 mismatch for {src}\n  expected {want}\n  got      {got}")
        return EXPECTED_APK_MD5, EXPECTED_ODEX_MD5
    return _digest(apk_src, "md5"), _digest(odex_src, "md5")


def _patched_stage_tree(staging, overlay_paths, meta, rom_type, overlay_dir, update_binary, sleep_binary, partitions):
    _ = overlay_paths, meta, overlay_dir

    apk_src, odex_src = _source_paths(rom_type)
    apk_md5, odex_md5 = _verify_source(rom_type)

    for rel, src in [
        ("system/vendor/app/DaLauncher.apk", apk_src),
        ("system/vendor/app/DaLauncher.odex", odex_src),
    ]:
        dst = staging / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)

    (staging / "system" / "vendor").mkdir(parents=True, exist_ok=True)
    upstream.write_lf(
        staging / "system" / "build.prop",
        f"ro.build.id={upstream.NEW_DA_VERSION}\nro.build.tags={upstream.NEW_DA_BUILD_TAGS}\n",
    )
    upstream.write_lf(staging / "system" / "vendor" / "build.prop", f"custom_rom.type={rom_type}\n")

    sleep_dst = staging / upstream.SLEEP_PKG_PATH
    sleep_dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(sleep_binary, sleep_dst)
    shutil.copy2(BUSYBOX_SRC, staging / "busybox")

    meta_dir = staging / "META-INF" / "com" / "google" / "android"
    meta_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(update_binary, meta_dir / "update-binary")
    (meta_dir / "updater-script").write_text(_updater_script(rom_type, partitions, apk_md5, odex_md5))


def _updater_script(
    rom_type: str,
    partitions,
    apk_md5: str = EXPECTED_APK_MD5,
    odex_md5: str = EXPECTED_ODEX_MD5,
) -> str:
    _ = partitions
    BB = "/tmp/busybox"
    USB = "/mnt/usbdrive1"
    LOG = f"{USB}/dalauncher-restore-{rom_type}.log"
    APK = "/system/vendor/app/DaLauncher.apk"
    ODEX = "/system/vendor/app/DaLauncher.odex"
    OKF = f"{USB}/ROLLBACK-RESTORED.OK"
    FAILF = f"{USB}/ROLLBACK-RESTORED.FAIL"

    def sh(cmd: str) -> str:
        esc = cmd.replace("\\", "\\\\").replace('"', '\\"')
        return f'run_program("{BB}", "sh", "-c", "{esc}");'

    # Dono e modo em /system pelo busybox, que só registra a falha no log: o
    # set_perm do update-binary empacotado aborta com "some changes failed"
    # quando o chown/chmod falha (ex.: o CAP não montou e o arquivo não foi
    # extraído).
    def perm(path: str, mode: str) -> str:
        return sh(
            f"{BB} chown 0:0 {path} 2>>{LOG}; c=$?; {BB} chmod {mode} {path} 2>>{LOG}; "
            f"{BB} echo perm {path} chown-exit=$c chmod-exit=$? >> {LOG}"
        )

    # Nada aqui pode abortar o updater-script: sem assert() nem abort(), e
    # set_perm só em /tmp. Se o recovery falha no meio, o comando estagiado em
    # /cache/recovery/command fica lá e a central entra em recovery loop (ic1101
    # docs/updates.md). Por isso a verificação grava .OK/.FAIL no pendrive em
    # vez de abortar.
    out = [
        "show_progress(1.000000, 0);",
        "set_progress(0.000000);",
        'package_extract_file("busybox", "/tmp/busybox");',
        'set_perm(0, 0, 0755, "/tmp/busybox");',
        sh(f"{BB} echo === DALAUNCHER-RESTORE START $({BB} date) rom_type={rom_type} === > {LOG} 2>&1"),
        sh(f"{BB} rm -f {OKF} {FAILF} 2>>{LOG}"),
        'mount("ext4", "EMMC", "/dev/block/platform/sdhci-tegra.3/by-name/APP", "/system");',
        'mount("ext4", "EMMC", "/dev/block/platform/sdhci-tegra.3/by-name/CAP", "/system/vendor");',
        sh(f"{BB} mount >> {LOG} 2>&1; {BB} echo mount-done >> {LOG}"),
        sh(f"{BB} md5sum {APK} {ODEX} >> {LOG} 2>&1; {BB} echo pre-restore-md5 >> {LOG}"),
        'package_extract_file("system/vendor/app/DaLauncher.apk", "/system/vendor/app/DaLauncher.apk");',
        perm(APK, "0644"),
        'package_extract_file("system/vendor/app/DaLauncher.odex", "/system/vendor/app/DaLauncher.odex");',
        perm(ODEX, "0644"),
        sh(f"{BB} md5sum {APK} {ODEX} >> {LOG} 2>&1; {BB} echo post-restore-md5 >> {LOG}"),
        # .OK só quando APK e odex restaurados têm o md5 do original da safra. É o
        # gate primário do §2.1 do checklist: ver o arquivo sem precisar ler o log.
        sh(
            f"if {BB} md5sum {APK} | {BB} grep -q {apk_md5} && {BB} md5sum {ODEX} | {BB} grep -q {odex_md5}; "
            f"then {BB} echo OK > {OKF}; {BB} echo restore-verified-OK >> {LOG}; "
            f"else {BB} echo FAIL > {FAILF}; {BB} echo restore-verify-FAILED >> {LOG}; fi"
        ),
        sh(f"{BB} sync"),
        'unmount("/system/vendor");',
        'unmount("/system");',
        sh(f"{BB} echo === DALAUNCHER-RESTORE END $({BB} date) === >> {LOG}; {BB} sync"),
        # Quebra o loop do daupdater: sem renomear o gatilho e apagar o
        # .copy_complete, ele reaplica o pacote a cada boot (MAPA §17.17).
        sh(f"{BB} mv {USB}/SwUpdate2.txt {USB}/SwUpdate2.txt.done 2>>{LOG}; {BB} echo mv-trigger-exit=$? >> {LOG}"),
        sh(f"{BB} rm -f /cache/.copy_complete 2>>{LOG}; {BB} echo rm-copy-complete-exit=$? >> {LOG}"),
        sh(f"{BB} sync"),
        f'package_extract_file("{upstream.SLEEP_PKG_PATH}", "/tmp/sleep");',
        'set_perm(0, 0, 0755, "/tmp/sleep");',
        "set_progress(1.000000);",
        'run_program("/tmp/sleep", "30");',
    ]
    return "\n".join(out) + "\n"


upstream.stage_tree = _patched_stage_tree


def main() -> None:
    parser = argparse.ArgumentParser(description="Build original DaLauncher restore OTA package")
    ota_common.add_rom_type_arg(parser)
    args = parser.parse_args()
    rom_type = args.rom_type or ROM_TYPE

    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    logger = logging.getLogger("civichmi-ota-dalauncher-restore")

    apk_md5, odex_md5 = _verify_source(rom_type)
    logger.info("source verified (rom_type=%s, md5 apk=%s odex=%s)", rom_type, apk_md5, odex_md5)

    build_dir = OUT_DIR / datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    build_dir.mkdir(parents=True, exist_ok=False)
    upstream.write_lf(build_dir / "SwUpdate2.txt", f"Ver:{upstream.NEW_COMMAND_VERSION}\n")

    signed = upstream.build_mdt(
        rom_type,
        OVERLAY_PATHS,
        OVERLAY_META,
        build_dir,
        overlay_dir=HERE / "overlay",
        update_binary=INPUT_DIR / "update-binary",
        sleep_binary=INPUT_DIR / "sleep" / "output" / "sleep",
        signapk_jar=INPUT_DIR / "signapk.jar",
        sign_cert=INPUT_DIR / "keys" / "testkey.x509.pem",
        sign_key=INPUT_DIR / "keys" / "testkey.pk8",
        partitions=upstream.PARTITIONS,
    )
    logger.info("dalauncher-restore mdt: %s", signed)
    print(f"OK: {build_dir}")


if __name__ == "__main__":
    main()
