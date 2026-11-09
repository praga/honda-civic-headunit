# Phase 7 — Final Deployment (2026-10-16)

Index document for the milestone session where the project achieved full success: AutoOk APK installed in `/system/app`, setuid `su` in `/system/xbin`, and the startup warning automatically dismissed on every vehicle boot on a Brazilian 2017 Civic.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../docs/fase7-deploy-2026-10-16/README.md)

---

## Result

The primary project objective was achieved. The startup "Warning" disclaimer (`The driver is responsible for safe driving...`) is automatically dismissed ~10 seconds after boot without requiring manual touch interaction. CarPlay, Bluetooth, FM/AM Audio, and Navigation remain completely intact.

---

## How We Got Here

- **No ADB needed:** The car's USB port did not enumerate ADB without Developer Mode, and Dev Mode was blocked by a passcord on Brazilian firmware.
- **No commercial tools:** No paid tools like Honda Hack ($25) were used.
- **No hardware soldering or JTAG required.**

The attack vector was **EvilValet** — a systematic design flaw in the Mitsubishi Andromeda recovery pipeline: the factory recovery environment accepts any `SwUpdate.mdt` signed with the public AOSP `testkey.pk8`. This yields arbitrary root execution via a standard USB flash drive plugged into the front USB-A port — provided the package matches the car's `custom_rom.type` and adheres to recovery package constraints.

**References:**
- **Public Attack Announcement:** [juniperspring.org/posts/honda-evil-valet](https://juniperspring.org/posts/honda-evil-valet/) by librick / Eric McDonald (2026-06-13).
- **Technical Specification:** `tooling/references/ic1101/docs/updates.md` by the same author.

**Our contribution** was the first public confirmation on Brazilian firmware (`1.F187.16`), discovery of `rom_type` `0040`, embedded busybox integration (required because BR recovery lacks `/sbin/sh`), and building an autonomous background auto-tap APK.

---

## Documents in This Folder

1. **[01-full-journey.md](01-full-journey.md)** — Chronological narrative of the 24-hour breakthrough: from initial failing builds to verified `tap 400,350 -> true` in logs.
2. **[02-technical-findings.md](02-technical-findings.md)** — Consolidated technical profile of Brazilian firmware `1.F187.16`. Hardware specs, kernel parameters, 50+ vendor APKs, framework libraries, and key combinations.
3. **[03-ota-build-recipe.md](03-ota-build-recipe.md)** — Step-by-step OTA package generation recipe (install + rollback). Monkey-patching `ic1101/ota-builder`, embedding busybox, installing setuid `su`, and Edify mounting.
4. **[04-autook-apk-details.md](04-autook-apk-details.md)** — Deep-dive on `AutoOk.apk`: BootReceiver → Service → LogcatWatcher → Shell, handling D8 dexer quirks, logcat needles, and persistent lifecycle management.
5. **[05-maintenance-and-future.md](05-maintenance-and-future.md)** — Day-to-day operations: updating the APK, applying emergency rollbacks, troubleshooting, and future enhancements.
6. **[06-obtained-dumps.md](06-obtained-dumps.md)** — Catalog of 197MB of filesystem dumps extracted from `/system` without ADB.

---

## Quick Reference Specs

| Field | Value |
|---|---|
| Head Unit | Honda Civic 2017 EXL BR, Display Audio 7" Mitsubishi Andromeda |
| DA Version | `1.F187.16` (release-keys, build 2016-04-07) |
| Rom Type | `0040` |
| Device | `vcm30t30` (NVIDIA Tegra 3) |
| Android | 4.2.2 (API 17) |
| Deployed APK | `/system/app/AutoOk.apk` (SHA-1 `e76d35741c5fafb8289f97f9565aaf8e1bff4f66`) |
| Deployed su | `/system/xbin/su` mode `06755` (setuid) |
