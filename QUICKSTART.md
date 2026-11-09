# 🚀 Quick Start — Deploy in 20 minutes

A quick-start guide to deploying to your Honda Civic G10 head unit (confirmed on BR firmware `1.F187.16`).

**For other firmware versions**, please follow [`docs/en/03-finding-your-firmware.md`](docs/en/03-finding-your-firmware.md) before this quickstart.

> ⚠️ **BEFORE YOU BEGIN, READ [DISCLAIMER.md](DISCLAIMER.md).**  
> Risk of permanent brick. Head unit replacement: ~$200–$400. You are 100% responsible.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](QUICKSTART.pt-BR.md)

---

## 🧭 Two deployment routes

The project currently provides two layers of solutions for the warning disclaimer. You can pick one or apply both in sequence.

| | 🔹 **Layer 1 — AutoOk** (safety net) | 🎯 **Layer 2 — Phase 10** (definitive solution) |
|---|---|---|
| **What it does** | APK + `su` in `/system/`. Monitors logcat, issues `input tap` on OK | 1-instruction smali patch in `DaLauncher.apk`. Forces skipping `DisclaimerFragment` |
| **Visual experience** | Warning appears and vanishes on its own in ~3 s | **Warning never appears.** "Ecoscore" screen for ~1 s (Phase 10.1) |
| **Risk** | Low (sidecar APK, zero touches on existing Mitsubishi apps) | Medium (modifies DaLauncher — main launcher; rollback package safety net) |
| **Effort** | ~20 min total | ~30 min (requires Layer 1 first or another root mechanism to validate the pipeline) |
| **Reversible** | Yes, via `prepare-pendrive-rollback.sh` without arguments | Yes, via `prepare-pendrive-rollback.sh tooling/ota-build/output-dalauncher-restore` |

**Recommendation:** Apply Layer 1 first, test for 1 week. Then apply Layer 2 on top. This QUICKSTART details Layer 1 step-by-step — Layer 2 follows the same OTA USB flash drive workflow; see full runbook in [`docs/en/fase10-deploy-2026-10-29/README.md`](docs/en/fase10-deploy-2026-10-29/README.md) and [`docs/fase10-execucao-2026-10-27.md`](docs/fase10-execucao-2026-10-27.md).

---

## 🛠️ Prerequisites

### Hardware

- **Honda Civic G10** 2017 EXL BR (firmware `1.F187.16` confirmed; other models untested)
- **USB-A flash drive** FAT32-formatted (any size ≥ 100MB works)
- **Linux PC / Laptop** (Fedora or Ubuntu preferred, macOS also works with appropriate tools)
- Physical access to the car

### PC Software

```bash
# Dependencies via package manager (Fedora)
sudo dnf install -y java-17-openjdk-devel android-tools git curl unzip parted dosfstools python3

# OR Ubuntu/Debian
sudo apt install -y openjdk-17-jdk android-tools-adb git curl unzip parted dosfstools python3
```

And Android SDK for `apksigner`:

```bash
mkdir -p ~/sdks/Android/cmdline-tools
cd ~/sdks/Android/cmdline-tools
curl -L -o cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdtools.zip && rm cmdtools.zip && mv cmdline-tools latest

export ANDROID_SDK_ROOT=~/sdks/Android
yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "build-tools;33.0.2"
```

---

## 📥 1. Clone project and dependencies

```bash
git clone https://github.com/khalleb/honda-civic-headunit.git
cd honda-civic-headunit

# Clone reference projects (ic1101 provides ota-builder)
mkdir -p tooling/references
git clone https://github.com/librick/ic1101.git tooling/references/ic1101
git clone https://github.com/codeage/root-honda.git tooling/references/root-honda
```

## 📦 2. Prepare binaries (busybox + su)

These binaries are sourced from the third-party reference projects cloned above:

```bash
mkdir -p tooling/ota-build/bin

# busybox from HondaHack (via root-honda packages)
# If HondaHack.apk is not in root-honda, download from another trusted source
# or compile busybox-static ARM yourself.
unzip -o tooling/references/root-honda/packages/HondaHack.apk -d /tmp/hondahack
cp /tmp/hondahack/res/raw/busybox tooling/ota-build/bin/busybox

# su from root-honda
cp tooling/references/root-honda/su tooling/ota-build/bin/su
```

## 🔨 3. Build the AutoOk APK

```bash
cd our-apk
./gradlew clean assembleRelease

# Sign with debug keystore
APKSIGNER=$ANDROID_SDK_ROOT/build-tools/33.0.2/apksigner
$APKSIGNER sign \
  --ks ~/.android/debug.keystore \
  --ks-pass pass:android \
  --out app/build/outputs/apk/release/AutoOk.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# Copy to the OTA package overlay
cp app/build/outputs/apk/release/AutoOk.apk ../tooling/ota-build/overlay/system/app/AutoOk.apk
cd ..
```

## 📦 4. Build the OTA package

```bash
# INSTALL package (AutoOk.apk + su)
python3 tooling/ota-build/build.py

# ROLLBACK package (removes AutoOk.apk and su — safety net)
python3 tooling/ota-build/build-rollback.py
```

Output:

```
INFO: rom_type=0040 -> tooling/ota-build/output/<timestamp>/0040/SwUpdate.mdt
OK: tooling/ota-build/output/<timestamp>
```

## 💾 5. Prepare the USB flash drive

Plug in the USB drive. Identify the device node (CAUTION — writing to the wrong device will wipe laptop drives!):

```bash
lsblk -o NAME,SIZE,MODEL,TRAN
```

Confirm which `/dev/sdX` corresponds to your flash drive (`TRAN=usb`). Then:

```bash
# Format as FAT32 MSDOS (wipes contents!)
sudo ./tooling/scripts/format-pendrive.sh /dev/sdX

# Write the INSTALL package
sudo ./tooling/scripts/prepare-pendrive-install.sh /dev/sdX

# (Optional, but RECOMMENDED) Flash another drive with ROLLBACK for emergency use
# or keep the rollback build on your laptop: tooling/ota-build/output-rollback/<timestamp>/
```

Unmount and remove the USB drive from your laptop.

---

## 🚗 6. Deploy in the car

1. **Insert the USB flash drive into the front USB-A port** of the head unit (near the driver/steering wheel, not inside the center armrest console).
2. **Turn car on** — key in ACC or ON position, engine off.
3. **Press and hold the `AUDIO` button** on the head unit for **5 seconds**. A dialog appears: "Do you want to restart?".
4. **Confirm restart**. The head unit begins the update flow:
   - Screen shows "Software update → Copying..." (progress bar 0% → 100%)
   - Head unit reboots automatically
   - May show recovery progress bar
   - Normal boot resumes
5. **Once the head unit returns to the normal screen** (or the Warning screen), **unplug the flash drive**.
6. **Tap OK one last time manually** on this first boot (the APK will start operating on the NEXT boot).

Total time: ~5–10 minutes for the OTA process.

## ✅ 7. Test

- **Turn off the car** (switch key to OFF).
- **Turn back on** (switch key to ACC or ON).
- The Warning screen appears normally.
- **~3–10 seconds later, it should dismiss automatically.**
- Test multiple times: turn off/on, off/on. It should intercept EVERY time.

If it works: **🎉 Project complete!** You never have to tap the disclaimer again.

---

## 🆘 Quick troubleshooting

| Symptom | Probable cause | Action |
|---|---|---|
| Holding AUDIO 5s doesn't prompt "Do you want to restart?" | Wrong key combo, very old firmware | Try pulling the radio fuse for 15 min and reconnecting — forces cold boot |
| "Software update" screen doesn't show up | USB drive not detected OR wrong `rom_type` | Test drive in another USB port, re-format; if it persists, dump logs via Error History |
| Warning screen appears but APK doesn't dismiss it | Service didn't start OR Activity logcat needle mismatch | See [detailed troubleshooting](docs/en/06-troubleshooting.md) |
| Head unit fails to boot afterwards | Something went wrong | **Deploy rollback package immediately.** See [`docs/en/07-rollback.md`](docs/en/07-rollback.md) |

Detailed troubleshooting guide: [`docs/en/06-troubleshooting.md`](docs/en/06-troubleshooting.md).

## 🔙 Rollback

If you ever need to revert, prepare the USB drive with the ROLLBACK package and apply it identically to INSTALL:

```bash
sudo ./tooling/scripts/prepare-pendrive-rollback.sh /dev/sdX
```

Then in the car: same protocol (front USB port + hold AUDIO 5s + confirm restart). The package cleans up `AutoOk.apk` and `su` from `/system`.

---

**Next steps & deep dives:**
- [`docs/en/00-overview.md`](docs/en/00-overview.md) — Complete architecture and overview
- [`docs/en/fase10-deploy-2026-10-29/README.md`](docs/en/fase10-deploy-2026-10-29/README.md) — **Layer 2 (Phase 10)**: DaLauncher smali patch, real deploy timeline, artifact SHAs/md5s
- [`docs/MAPA.md`](docs/MAPA.md) / [`docs/en/MAP.md`](docs/en/MAP.md) — Head unit navigable system atlas
- [`DISCOVERIES.md`](DISCOVERIES.md) — Technical findings index
