# Complete Journey — 2026-10-15 to 2026-10-16

Chronological narrative of the breakthrough and initial deployment. Documents the research, trial, error, and rapid iteration process that led from the initial hypothesis ("EvilValet might work on our firmware") to a production-grade automated deployment.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/01-jornada-completa.md)

---

## Initial State — Night of 2026-10-15

The project stood at:
- An auto-OK APK compiled with 3 placeholder constants (`AVISO_ACTIVITY_NAME=""`, `BUTTON_OK_X=-1`, `BUTTON_OK_Y=-1`).
- Reproducible Fedora build toolchain (JDK 17 Temurin, Android SDK, Gradle 7.6.4).
- Mitsubishi software architecture analyzed (whitelist, Honda Installer).
- Brazilian EXL service menu key combo confirmed.
- ~20 B-CAN signals mapped; 64 `com.mitsubishielectric.ada.*` packages cataloged.

The ADB route via settings passcord was completely blocked: on Brazilian firmware `1.F187.16`, the `1-4-2-3` text sequence was patched out by Honda/Mitsubishi in early firmware updates.

A broader survey of alternative vectors identified **EvilValet** as the most promising avenue ([`docs/en/01-attack-architecture.md`](../01-attack-architecture.md)): Honda left the public AOSP `testkey.x509.pem` inside `res/keys` of the recovery partition. The recovery binary's `verify_file` function accepts any `update.zip` signed with the standard public AOSP `testkey.pk8`.

Prior public confirmations:
- Civic 2021 US (`1.F1A5.15`, rom_type `1115`) — by librick.
- EU update file `MRC_EU_SW_v12_4.zip` (`1.F197.70`, rom_type `2250`).
- Brazilian firmware `1.F187.16` had never been publicly tested or documented.

Session goal: Test EvilValet on Brazilian firmware.

---

## First Attempt — Narrow `rom_type` Guess (~20:00 BRT)

We built an initial package testing known rom_types `1110`, `1115`, `1118`, `1120`, `2250`. Formatted a FAT32 MSDOS partition USB drive, plugged it into the front USB port, and turned the key to ACC.

**Result:** Nothing happened. The normal warning appeared.

**Key discovery #1:** Turning the ignition to ACC **is not a cold boot**. The Honda head unit enters LP0 deep suspend when the engine turns off; turning the key merely wakes from suspend. The recovery updater daemon (`daupdater`) runs **exclusively on cold boots**.

---

## Discovering the True `rom_type` (`0040`)

To determine the actual `rom_type` without guessing:
1. We triggered a forced cold boot: **hold the `AUDIO` button for 5 seconds** and select "Restart".
2. We extracted crash/system logs via the service menu: `DAY/NIGHT + AUDIO + MENU` for 2s → `Detail Information & Setting` → `Error History` → `Save`.
3. In the saved logcat, we spotted the crucial line:
   ```
   daupdater: Can't open /mnt/usbdrive1/0040/SwUpdate.mdt
   ```
4. **`rom_type` is `0040`!** Completely outside the previously speculated 11xx/22xx ranges.

---

## Embedded Busybox Requirement

Upon packaging for `rom_type` `0040`, the recovery executed, but scripts failed to run commands.

**Key discovery #2:** The Brazilian recovery partition does not ship `/sbin/sh` or standard shell utilities.

**Solution:** Embed a statically linked ARM `busybox` binary (extracted from HondaHack resources) into the OTA payload, unpack it into `/tmp/busybox`, and invoke commands via `run_program("/tmp/busybox", ...)`.

---

## Building AutoOk & In-Car Success

With root execution established:
1. Re-structured `AutoOk.apk` to use explicit named inner classes (bypassing legacy D8 compiler issues on Android 4.2.2).
2. Identified target Activity: `com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity`.
3. Identified button coordinates: `(400, 350)` on the 800×480 touchscreen.
4. Installed `AutoOk.apk` into `/system/app/` and `su` into `/system/xbin/`.
5. Built install OTA package (`tooling/ota-build/build.py`) and rollback package (`build-rollback.py`).
6. Deployed in-car via USB flash drive.

### The Persistence Fix
Initially, the service called `stopSelf()` after the first tap, meaning subsequent LP0 resumes failed to dismiss the warning. We refactored `AutoOk` to add `rearmWatcher()`, keeping the service permanently active across ignition cycles.

**Final Result:**
Turn on car → warning appears → dismissed automatically within seconds → 100% stable across repeated trips!
