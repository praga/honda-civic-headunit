# AutoOk — Startup Warning Auto-Dismiss APK

Autonomous Android APK designed to dismiss the "Warning / Disclaimer" screen on Honda Civic G10 EXL startup. Implements the algorithm without relying on paid commercial tools like Honda Hack. Package name: `com.khalleb.civichmi.autook`.

*Language:* 🇺🇸 **English**

---

## Overview & Architecture

AutoOk runs as a persistent service on the head unit to automatically send an input tap whenever the disclaimer screen appears.

### Configuration (`AutoOkConfig.java`)

| Parameter | Value | Description |
|---|---|---|
| `AVISO_ACTIVITY_NAME` | `OpeningDivideActivity` | The Activity launched on startup (`com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity`) |
| `BUTTON_OK_X` | `400` | X-coordinate of OK button on 800×480 screen |
| `BUTTON_OK_Y` | `350` | Y-coordinate of OK button on 800×480 screen |
| `DELAY_SECONDS_AFTER_DETECT` | `2` | Delay in seconds after detection before issuing tap |
| `BOOT_INITIAL_WAIT_MS` | `5000` | Initial wait after `BOOT_COMPLETED` before monitoring logcat |

### How It Works

1. `BootReceiver` listens for `BOOT_COMPLETED` (and `HmiServiceReceiver` listens for `ACTION_HMI_SERVICE_START`) to launch `AutoOkService`.
2. `AutoOkService` validates configuration.
3. `LogcatWatcher` executes `su -c "logcat -v brief ActivityManager:I *:S"` on a background thread and scans line-by-line for `AVISO_ACTIVITY_NAME`.
4. When detected, it invokes the service callback, waits `DELAY_SECONDS_AFTER_DETECT`, and triggers `su -c "input tap X Y"` via `Shell`.
5. Re-arms watcher for subsequent resume cycles (LP0 sleep/wake).

---

## Compilation

Prerequisites: Android SDK (API 33 compileSdk; build-tools 33.0.2), `ANDROID_SDK_ROOT` set.

```bash
cd our-apk
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`.

Sign with any keystore (debug keystore is fine):

```bash
APKSIGNER=$ANDROID_SDK_ROOT/build-tools/33.0.2/apksigner
$APKSIGNER sign \
  --ks ~/.android/debug.keystore \
  --ks-pass pass:android \
  --out AutoOk-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Installation via OTA Package (Recommended)

Copy `AutoOk-signed.apk` to `tooling/ota-build/overlay/system/app/AutoOk.apk` and run:

```bash
python3 tooling/ota-build/build.py
```

Then flash via USB drive as described in [`QUICKSTART.md`](../QUICKSTART.md).

---

## Viewing Logs

```bash
adb logcat -s AutoOk:*
```

Key log signatures:
- `watcher started, waiting for <Activity>` — Service started with valid config.
- `tap X,Y -> true` — Tap issued with exit status 0.
- `tap X,Y -> false` — Tap failed or permission denied.

---

## Module Structure

```
our-apk/
├── build.gradle               (AGP 7.4.2)
├── settings.gradle
├── gradle.properties
├── README.md                   (this file)
└── app/
    ├── build.gradle           (minSdk/targetSdk 17, compileSdk 33)
    ├── proguard-rules.pro
    └── src/
        ├── test/java/com/khalleb/civichmi/autook/
        │   ├── AvisoMatcherTest.java   (unit tests for needle matching)
        │   └── TapDeduperTest.java     (unit tests for dedup timing)
        └── main/
            ├── AndroidManifest.xml
            ├── res/values/strings.xml
            └── java/com/khalleb/civichmi/autook/
                ├── AutoOkConfig.java     (constants & coordinates)
                ├── BootReceiver.java     (BOOT_COMPLETED listener)
                ├── HmiServiceReceiver.java (ACTION_HMI_SERVICE_START listener)
                ├── AutoOkService.java    (main lifecycle coordinator)
                ├── LogcatWatcher.java    (logcat stream parser)
                ├── TapDeduper.java       (tap deduplication)
                ├── AvisoMatcher.java     (Activity matcher)
                └── Shell.java            (su tap execution)
```
