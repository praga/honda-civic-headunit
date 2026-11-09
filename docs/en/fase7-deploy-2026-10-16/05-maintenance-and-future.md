# Maintenance, Troubleshooting & Future Roadmap

Operational runbook for maintaining the deployment, updating APKs, applying rollbacks, and troubleshooting.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/05-manutencao-e-futuro.md)

---

## Rebuilding & Redeploying the APK

When changes are made to `our-apk` Java source or `AutoOkConfig.java`:

```bash
# 1. Clean and build APK
cd our-apk
./gradlew clean assembleRelease

# 2. Sign with debug keystore
APKSIGNER=$ANDROID_SDK_ROOT/build-tools/33.0.2/apksigner
$APKSIGNER sign \
  --ks ~/.android/debug.keystore \
  --ks-pass pass:android \
  --out app/build/outputs/apk/release/AutoOk.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# 3. Copy to OTA overlay
cd ..
cp our-apk/app/build/outputs/apk/release/AutoOk.apk \
   tooling/ota-build/overlay/system/app/AutoOk.apk

# 4. Rebuild install OTA package
python3 tooling/ota-build/build.py

# 5. Flash to USB drive
sudo ./tooling/scripts/prepare-pendrive-install.sh /dev/sdX
```

Then flash in-vehicle using the standard protocol (hold `AUDIO` for 5s).

---

## Complete Rollback Procedure

If you ever need to completely remove custom modifications:

```bash
# Flash the rollback package to your USB drive
sudo ./tooling/scripts/prepare-pendrive-rollback.sh /dev/sdX
```

In the car:
1. Insert flash drive into front USB port.
2. Hold `AUDIO` for 5s → confirm restart.
3. Recovery executes `busybox rm -f /system/app/AutoOk.apk /system/xbin/su`.
4. Head unit reboots back into 100% factory state.

---

## Handling Dealership Firmware Updates

If an official Honda dealership update is applied:
- Official OTA flashing will overwrite `/system`, removing custom files.
- Simply re-insert your installation flash drive and execute the cold boot update protocol again to restore modifications.
