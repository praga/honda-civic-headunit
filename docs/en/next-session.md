# Next Session — Project Reference & Entry Points

> **Project successfully completed.** The startup Warning disclaimer on the Civic 2017 EXL BR
> (`1.F187.16`, Mitsubishi Andromeda, Android 4.2.2) is dismissed automatically on every boot — zero manual interaction required.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../proxima-sessao.md)

---

## What Was Done

- `AutoOk.apk` in `/system/app/` — monitors logcat, detects Warning Activity, triggers automated tap via `su -c "input tap 400 350"`.
- Setuid ARM `su` in `/system/xbin/` — provides permanent root access.
- Deployed via EvilValet: OTA package `SwUpdate.mdt` signed with public AOSP testkey.
- Phase 10: 1-instruction smali patch in `DaLauncher.apk` completely removing the disclaimer from the boot sequence.

---

## Documentation Index

Primary folder: **[`docs/en/`](README.md)**

| Area | Documentation |
|---|---|
| Quick Start Guide | [`../../QUICKSTART.md`](../../QUICKSTART.md) |
| Architecture Overview | [`00-overview.md`](00-overview.md) |
| Phase 10 Smali Patch | [`fase10-deploy-2026-10-29/README.md`](fase10-deploy-2026-10-29/README.md) |
| Phase 7 Deployment | [`fase7-deploy-2026-10-16/`](fase7-deploy-2026-10-16/) |
| Platform X-Ray (AIDL & Broadcasts) | [`raio-x/`](raio-x/) |
| Head Unit System Atlas | [`MAP.md`](MAP.md) |
| Technical Findings | [`../../DISCOVERIES.md`](../../DISCOVERIES.md) |
| Development Roadmap | [`../../ROADMAP.md`](../../ROADMAP.md) |

---

## Frequently Asked Questions

### "How do I update the APK?"
→ [`fase7-deploy-2026-10-16/05-maintenance-and-future.md`](fase7-deploy-2026-10-16/05-maintenance-and-future.md)

### "How do I perform an emergency rollback?"
→ [`07-rollback.md`](07-rollback.md)

### "What if a dealership OTA update removes modifications?"
→ Re-flash via the install flash drive following [`05-deploying-in-car.md`](05-deploying-in-car.md).

---

## Technical Specifications Summary

| Field | Value |
|---|---|
| Rom type (EvilValet key) | `0040` |
| Device | `vcm30t30` (Tegra 3) |
| DA version | `1.F187.16` |
| Cold boot combo | `AUDIO 5s` + confirm restart |
| Warning OK button coords | X=400, Y=350 (800×480 screen) |
| Warning Activity | `com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity` |
| `/system` partition block | `/dev/block/platform/sdhci-tegra.3/by-name/APP` (`mmcblk0p3`) |
| AOSP testkey SHA-256 | `A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC` |
