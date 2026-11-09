# Briefing — Honda Civic G10 Head Unit Project

> **🎉 PROJECT COMPLETED ON 2026-10-16 & 2026-10-29.** Primary goal achieved: Startup Warning disclaimer is automatically bypassed on every ignition cycle. Zero manual interaction required.
>
> This briefing provides context for a new AI session regarding the final state and entry points.

*Language:* 🇺🇸 **English**

---

## 📋 Prompt to Paste into a New AI Session

```
Hello. I am working on the reverse engineering project for the Brazilian Honda Civic 2017 (G10) EXL factory head unit.
The project was completed successfully: the startup "Warning" disclaimer screen is dismissed automatically on every boot.

Project documentation:
    docs/en/ (primary English documentation)
    docs/MAPA.md or docs/en/MAP.md (head unit atlas)
    docs/en/fase10-deploy-2026-10-29/ (Phase 10 deployment)

Technical Specs:
- Head Unit: 1.F187.16, Mitsubishi Andromeda, Android 4.2.2, Tegra 3 vcm30t30
- Rom type (EvilValet key): 0040
- Cold boot combo: hold AUDIO for 5s + confirm restart
- Warning OK button: center at (400, 350) on 800×480 screen
- Warning Activity: com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity
- /system partition: /dev/block/platform/sdhci-tegra.3/by-name/APP (= mmcblk0p3)
- APK at /system/app/AutoOk.apk
- su at /system/xbin/su (mode 06755 setuid)
- Phase 10: 1-instruction smali patch in DaLauncher.apk bypassing DisclaimerFragment

Deployment:
- EvilValet route (AOSP testkey + USB OTA package) accepted by Brazilian recovery.
- Reversible via rollback package.
```

---

## 🗂 Final Project State

- **Milestone Dates:** 2026-10-16 (Phase 7 AutoOk), 2026-10-29 (Phase 10 smali patch).
- **Result:** 100% functional in-vehicle testing across repeated ignition cycles.
- **Route:** EvilValet OTA update with embedded busybox.
- **Realized brick risk:** Zero. All changes are verified via rollback packages.

## 🚦 Phase Summary

| Phase | Description | Status |
|---|---|---|
| 0 | Environment Setup | ✅ Complete |
| 1 | In-car reconnaissance & Self-Diagnosis | ✅ Complete |
| 2 | Hardware connection & flash drive | ✅ Complete |
| 3 | ADB extraction | ❌ Replaced by EvilValet OTA dump |
| 4 | Static analysis of APKs | ✅ Warning Activity & layout mapped |
| 5 | AutoOk APK implementation | ✅ Built, signed, deployed |
| 5b | EvilValet OTA package creation | ✅ Scripted in `tooling/ota-build/build.py` |
| 6 | Head unit root access | ✅ Statically linked `su` via OTA |
| 7 | Full in-car deployment | ✅ AutoOk running in production |
| 10 | DaLauncher smali patch | ✅ Warning eliminated from boot flow |
