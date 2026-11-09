# Knowledge Base — Honda Civic G10 Head Unit (2016–2017)

> **🎉 PROJECT COMPLETED ON 2026-10-16 & 2026-10-29.** The primary goal was achieved: the startup Warning disclaimer
> is automatically suppressed on every vehicle start without manual interaction. The auto-OK APK + root access (via
> setuid `su`) are deployed to `/system/app/` and `/system/xbin/`, and Phase 10 bypassed the disclaimer smali bytecode directly.
>
> **Complete documentation of the deployment phases:** [`docs/en/`](docs/en/README.md)
> — architecture overview, technical findings, OTA package build recipes, APK details,
> maintenance and rollback procedures, and extracted dumps.
>
> *Language:* 🇺🇸 **English** · [🇧🇷 Português](central-multimidia-civic-g10-kb.pt-BR.md)
>
> The contents below document the **historical discovery log** (research, hypotheses, static analysis) conducted
> prior to the final deployment. Preserved as technical reference and context.

---

## Target Unit Profile

- **Vehicle:** Honda Civic 2017 **EXL**, manufactured in Brazil (VIN prefix `93H...`).
- **Head Unit:** 7" Display Audio (capacitive side buttons AUDIO/HOME/+VOL-/MENU/BACK; physical DAY/NIGHT button above screen — **no numbered presets 1-6 and no rotary volume knob**, which belong to LX trims).
- **Verified via Settings → About Device & Service Menu (2026-10-15):**
  - Model Number: **`MY16ADA`** (Model Year 2016, Android Display Audio)
  - Software (Version Number): **`1.F187.16`**
  - Android: **4.2.2** build `-55`
  - Kernel: `3.1.10+` build `jenkins@NDYWY3861584 #1 Thu Apr 7 18:50:54 JST 2016`
  - Bootloader **DBOOT**: `1.10_20160407183722`
  - Software Loader: `1.F187.16`
  - System µCom: `H62F0N 71.55.13` (Renesas/Hitachi family)

---

## 1. Hardware Architecture

- **SoC: NVIDIA Tegra 3** (quad-core ARM Cortex-A9 ARMv7, ~2012)
- **HMI Framework: Mitsubishi** (codename "Andromeda")
- **Android System Namespace: `com.mitsubishielectric.ada.*`** (older units used Clarion namespaces; Mitsubishi acquired Clarion in 2015)
- **NVIDIA Board: VCM30T30**
- Factory wired CarPlay / Android Auto supported.

### Note: Civic 2016–2021 Uses the Same Platform
The head unit hardware architecture is consistent across the 2016–2021 generation (excluding base monochrome trims). Consequently:
- All reverse engineering findings on Android 4.2.2 Andromeda apply broadly across model years.
- Boot/recovery images and partition layouts follow identical Tegra 3 partition schemes.

---

## 2. Software Architecture & USB Modes

- **Android Version:** 4.2.2 Jelly Bean (API 17).
- **Key System Packages:**
  - `com.mitsubishielectric.ada.app.dalauncher` — Main system launcher
  - `com.mitsubishielectric.ada.app.dasettings` — Settings application
  - `com.mitsubishielectric.ada.app.developerdiag` — Developer diagnostics
  - `com.mitsubishielectric.ada.app.systemupdate` — System update daemon (`daupdater`)
  - `com.mitsubishielectric.ada.appservice.powermanager` — Power manager (LP0 suspend/resume)
  - `com.mitsubishielectric.ada.appservice.camera` — Camera IPC and display service

### USB Modes (Critical Architectural Detail)
The head unit USB port supports two mutually exclusive modes:
- **Host Mode (Default):** Powers and reads connected devices (iPhone, USB flash drives). Required for CarPlay and USB updates.
- **Device Mode:** Head unit acts as a USB peripheral to a PC. Required for ADB connections.
Switching to Device Mode cuts off power delivery and disables CarPlay until restored to Host Mode.

---

## 3. The Startup Warning Screen

The disclaimer screen displayed on vehicle startup:
- Displays legal liability text regarding driver attention.
- Requires tapping "OK" before navigation or CarPlay projection can be accessed.
- Community testing proved that engineering menus reset state on every ignition cycle by design; **there is no persistent configuration toggle in software menus to disable the disclaimer**.
- Permanent suppression requires either:
  1. An autonomous service issuing simulated input taps (implemented in **AutoOk APK**).
  2. Bytecode smali patching of `DaLauncher.apk` (implemented in **Phase 10**).

---

## 4. Diagnostic Menus & Access Procedures

### 4.1 Self-Diagnosis Mode (Civic EXL BR)
1. Turn ignition to **ON**, audio **OFF**.
2. Press and hold simultaneously for ~2 seconds: **`DAY/NIGHT` (physical) + `AUDIO` (capacitive) + `MENU` (capacitive)**.
3. Opens **Self-Diagnosis Mode** menu:
   - `Self-Diagnosis Mode`: Automated component checks.
   - `Detail Information & Setting`: Deep hardware status, CAN bus live monitors, version manifests, and error history logs.

### 4.2 Cold Boot Combination
Turning the car key off only triggers LP0 deep sleep (suspend). To trigger a true cold reboot:
1. Press and hold the **`AUDIO`** power button for **5 seconds**.
2. When the dialog prompts "Do you want to restart?", select **Yes**.

---

## 5. CAN Bus Integration (F-CAN & B-CAN)

The head unit interfaces with two primary vehicle networks:
- **F-CAN (Fast / Powertrain):** High-speed bus handling vehicle speed, transmission, and ADAS telemetry.
- **B-CAN (Body):** Body control network carrying steering wheel switch inputs (`ST_SW_PAD`), illumination state (`ILLUMI`), reverse gear signal (`AT`), odometer (`ODO_TRIP`), and climate control (`ACSTATE`).

---

## 6. Root & Write Access via EvilValet

The head unit enforces strict system partition write protection during normal operation. The **EvilValet** vector exploits the recovery update framework:
1. The factory recovery partition trusts packages signed with the public AOSP test key (`testkey.pk8`).
2. When `daupdater` detects a properly formatted update payload (`SwUpdate.mdt`) matching the vehicle's `rom_type` (`0040`) on a cold boot, it applies the update automatically.
3. This grants arbitrary root execution during recovery, enabling APK installation and binary placement in `/system`.

---

## 7. Historical Research Conclusions

1. Manual Settings passcord sequences (`1-4-2-3`) are patched out on post-2016 Brazilian firmware.
2. The recovery partition accepts public AOSP testkeys, providing an unblockable, fully reversible USB deployment vector.
3. `AutoOk.apk` provides non-invasive auto-tap suppression via background logcat monitoring.
4. Phase 10 smali patching of `DaLauncher.apk` achieves clean, permanent disclaimer elimination at the bytecode level.
