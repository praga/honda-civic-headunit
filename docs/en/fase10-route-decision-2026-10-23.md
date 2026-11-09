# Phase 10 — Route Decision: Route B (AOSP Resign)

> Decision made on 2026-10-23 following analysis of DaLauncher's `AndroidManifest.xml`
> and `/system/etc/permissions/com.mitsubishielectric.ada.util.vehiclerw.lib.xml`.
> Route A (odex patch) was discarded due to added complexity without real benefit.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../docs/fase10-decisao-rotas-2026-10-23.md)

---

## Objective

Select between the two deployment routes planned in [`fase10-pre-planning-2026-10-20.md`](fase10-pre-planning-2026-10-20.md) to apply the smali patch that eliminates the startup warning disclaimer.

- **Route A:** Odex-level patch; APK remains untouched with original Mitsubishi signature.
- **Route B:** Re-sign with public AOSP testkey; DaLauncher is treated as a "new install".

---

## DaLauncher Manifest Analysis

Source: `tooling/apks-decoded/DaLauncher/AndroidManifest.xml` (deodexed locally via `tooling/deodex/deodex-apk.sh`).

### Verified Points

| Check | Result |
|---|---|
| `android:sharedUserId` | **Absent** — no shared UID to break |
| `android:process` of `<application>` | Default (package's own process) |
| 18× Mitsubishi `uses-library` | Independent of signature matching |
| Permissions declared by APK | Privileged, dangerous, normal (no pure signature permissions consumed by third parties) |

### Required Permissions (`uses-permission`)

| Permission | Level | Behavior under AOSP testkey |
|---|---|---|
| `com.mitsubishielectric.ada.permission.VEHICLE_RW` | **GID-only** (`<group gid="vehicle_rw"/>`, no `protectionLevel`) | ✅ Granted by declaration, independent of signature |
| `android.permission.CALL_PHONE` | Dangerous | ✅ Granted automatically in `/system` |
| `android.permission.SET_WALLPAPER`, `SET_WALLPAPER_HINTS` | Normal | ✅ Granted |
| `android.permission.VIBRATE` | Normal | ✅ Granted |
| `android.permission.BIND_APPWIDGET` | `signature\|system` | ✅ `\|system` covers it (APK lives in `/system/vendor/app`) |
| `android.permission.GET_ACCOUNTS` | Normal | ✅ Granted |
| `android.permission.ACCESS_FINE_LOCATION` | Dangerous | ✅ Granted |
| `android.permission.GET_TASKS` | `normal` in API 17 | ✅ Granted |
| Launcher internal permissions (`READ_SETTINGS`, etc.) | Normal / Dangerous | ✅ Granted |

### Critical Finding: `VEHICLE_RW` is Not a Signature-Level Permission

The most sensitive permission required by DaLauncher is Mitsubishi's `VEHICLE_RW`. The permission definition file at `/system/etc/permissions/com.mitsubishielectric.ada.util.vehiclerw.lib.xml` declares it **without `protectionLevel`**, mapping directly to UNIX group `vehicle_rw`:

```xml
<permission name="com.mitsubishielectric.ada.permission.VEHICLE_RW">
    <group gid="vehicle_rw" />
</permission>
<library name="com.mitsubishielectric.ada.util.vehiclerw.lib" file="/vendor/framework/VehicleRW.jar"/>
```

This pattern assigns the `vehicle_rw` UNIX GID to the process simply because it declared `<uses-permission>`. **Signature is not part of the gate.**

---

## Secondary Risk & Mitigation

Re-signing with the AOSP testkey triggers a signature mismatch in PackageManager on first boot:
1. `packages.xml` contains original Mitsubishi signature (`93:14:CE:...:05:AD`).
2. New APK bears AOSP testkey signature (`A4:0D:A8:...:F5:DC`).

Standard AOSP 4.2 behavior for system apps with altered signatures is `deletePackageLI` + `scanPackageLI`/`SCAN_NEW_INSTALL` — accepts the new APK, updates the signature in `packages.xml`, and clears `/data/data`.

Operational impact:
- Any custom wallpaper or widget layout in `/data/data/com.mitsubishielectric.ada.app.dalauncher/*` resets to defaults.
- First boot takes ~10–30s longer due to `dexopt` compiling `classes.dex`.

Neither impact represents a blocker in our automotive context.

---

## Final Decision

**Route B.** Route A introduces unnecessary complexity (host re-odexing for Dalvik 4.2.2 ARM). Route B:
- Leverages our existing, proven build pipeline (`apksigner`, EvilValet OTA package signed with AOSP testkey).
- Does not break any permissions.
- Risk is strictly bounded and automatically recoverable.

---

## Operational Requirements for OTA Package (`build-dalauncher-fase10.py`)

1. **Replace `/system/vendor/app/DaLauncher.apk`** with the rebuilt, AOSP-testkey-signed version.
2. **Remove `/system/vendor/app/DaLauncher.odex`** prior to first boot. The script outputs `FASE10-ODEX-REMOVED.OK` to verify removal.
3. **Mount `/system` + `/system/vendor`** (`CAP` partition).
4. **Mandatory loop break:** `mv SwUpdate2.txt .done` and `rm /cache/.copy_complete`.
5. **Log APK md5 checksum** to USB drive for audit verification.

---

## Pre-Execution Rollback

[`tooling/ota-build/build-dalauncher-restore.py`](../../tooling/ota-build/build-dalauncher-restore.py) restores original binary bytes from the initial dump:
- `DaLauncher.apk`: `1d8bd729ad309240bc77c6c2d5f9a93c07940cc355d4adbed9930ee6d9cee019`
- `DaLauncher.odex`: `1497dc8497799faf8e86fe8f90fede54dd98502472f1c3e03b82b4542eb57db7`
