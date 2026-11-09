# Technical Specifications: Firmware `1.F187.16` (BR)

Consolidated technical specifications of the Brazilian 2017 Civic EXL Display Audio (Mitsubishi Andromeda). Primary reference for future reverse engineering on this and related head units.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/02-descobertas-tecnicas.md)

---

## 0. Novel Discoveries from This Project

This project produced the **first public confirmed deployment of EvilValet on Brazilian firmware** (`1.F187.16` BR, 2026-10-16). Specific findings unique to the BR firmware not found in prior publications:

| Finding | Significance |
|---|---|
| **Rom type `0040`** for `1.F187.16` BR | Key required for EvilValet payload matching |
| **Combo `AUDIO 5s`** for forced cold boot | Enables `daupdater` execution (ignition toggle is suspend-only) |
| **Recovery lacks `/sbin/sh`** | Updater-script requires embedded ARM busybox |
| **Relaxed version check in `SystemUpdate`** | Accepts arbitrary `NEW_DA_VERSION` strings without `1.F1` prefix |
| **`MITSU` partition (`mmcblk0p5`)** | Unique partition present on Brazilian firmware |
| **`BOOT_COMPLETED` behavior on wake** | Does not fire on LP0 resume; requires persistent watcher |

---

## 1. Vehicle & Hardware Specs

| Field | Value | Source |
|---|---|---|
| Make / Model | Honda Civic EXL | — |
| Model Year | 2017 | — |
| Market | Brazil | — |
| Head Unit | Honda Display Audio 7" (Mitsubishi Andromeda) | — |
| SoC | NVIDIA Tegra 3 (ARM Cortex-A9 quad-core) | `kmsgcat.txt` |
| RAM | 1021 MB (`mem=1021M@2048M`) | kernel cmdline |
| Primary eMMC | 7.03 GiB (`mmcblk0: mmc0:0001 VM010B`) | kernel dmesg |
| Secondary Flash | 15.0 GiB (`mmcblk1: mmc2:cc7d RP16G`) | kernel dmesg |
| NOR Flash | ~64 MB (recovery/boot/misc) | kernel cmdline |
| Display Resolution | 800×480, LVDS | `hwcomposer` |

---

## 2. Software Architecture

### 2.1 `/system/build.prop` Highlights

```properties
ro.build.version.release=4.2.2
ro.build.version.sdk=17
ro.build.id=1.F187.16
ro.build.version.incremental=55
ro.build.type=user
ro.build.tags=release-keys
ro.product.model=MY16ADA
ro.product.brand=Honda
ro.product.device=vcm30t30
ro.product.board=Andromeda
ro.product.cpu.abi=armeabi-v7a
ro.product.cpu.abi2=armeabi
```

### 2.2 `/system/vendor/build.prop` (`CAP` partition)

```properties
custom_rom.type=0040
custom_rom.id=039726af3d8671c3855d21a9832e5214
```

---

## 3. Storage & Partitions

```
mmcblk0p1: UDA (user data)
mmcblk0p2: CAP (/system/vendor)
mmcblk0p3: APP (/system)
mmcblk0p4: MAP (navigation data)
mmcblk0p5: MITSU (vendor-specific)
mmcblk0p6: LOG (system logs)
```

---

## 4. Key Physical Button Combinations

- **Forced Cold Boot:** Hold **`AUDIO`** for **5 seconds** → Select "Yes" on prompt.
- **Service Menu (Self-Diagnosis):** Hold **`DAY/NIGHT + AUDIO + MENU`** for **2 seconds** with audio off.
- **Dump Logs:** Service Menu → `Detail Information & Setting` → `Error History` → `Save` (writes logcat and dmesg to USB).
