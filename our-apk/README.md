# AutoOk — Startup Warning Auto-Dismiss & Audio/System Enhancements APK

Autonomous Android APK designed to dismiss the "Warning / Disclaimer" screen on Honda Civic G10 EXL startup, plus native automotive audio and system enhancements. Package name: `com.khalleb.civichmi.autook`.

*Language:* 🇺🇸 **English**

---

## Overview & Architecture

AutoOk runs as a persistent service on the head unit to automatically dismiss the disclaimer screen and provide essential automotive enhancements:

1. **Auto-Dismiss Disclaimer**: Monitors logcat events for `OpeningDivideActivity` and sends touch input to dismiss the startup warning.
2. **Reverse Camera Volume Ducking**: Automatically ducks music volume by a configurable step (default 6 levels) whenever the reverse camera or LaneWatch (`RearWideCameraDisplayActivity` / `MvcDisplayActivity`) opens, and restores previous volume on exit (with a 30-second watchdog fail-safe).
3. **Audio Ducking / Mute Glitch Auto-Fix (`AudioMuteGuard`)**: Detects and fixes the notorious Civic head unit bug where CarPlay / Siri / Honda navigation voice ducking leaves `STREAM_MUSIC` stuck at volume 0.
4. **Auto-Start Preferred App (`AutoAppLauncher`)**: Automatically boots into a configured navigation or media app (e.g. Spotify, Waze) after disclaimer dismissal, optionally dispatching `KEYCODE_MEDIA_PLAY` (126).
5. **GPS System Time Auto-Sync (`GpsTimeSync`)**: Listens to GPS satellite fixes and automatically synchronizes the system clock via `date -s` and hardware RTC (`hwclock -w`) when clock drift exceeds 5 seconds.

### Configuration (`AutoOkConfig.java`)

| Parameter | Value | Description |
|---|---|---|
| `AVISO_ACTIVITY_NAME` | `OpeningDivideActivity` | The Activity launched on startup (`com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity`) |
| `BUTTON_OK_X` | `400` | X-coordinate of OK button on 800×480 screen |
| `BUTTON_OK_Y` | `350` | Y-coordinate of OK button on 800×480 screen |
| `DELAY_SECONDS_AFTER_DETECT` | `1` | Delay in seconds after detection before issuing tap |
| `BOOT_INITIAL_WAIT_MS` | `500` | Initial wait after `BOOT_COMPLETED` before monitoring logcat |
| `ENABLE_REVERSE_CAMERA_DUCK` | `true` | Enable auto volume reduction during reverse camera |
| `REVERSE_CAMERA_DUCK_STEPS` | `6` | Number of volume levels to decrease when camera active |
| `ENABLE_AUDIO_MUTE_GUARD` | `true` | Enable automatic recovery from stuck mute glitch |
| `ENABLE_AUTO_START_APP` | `false` | Enable launching preferred app on boot |
| `AUTO_START_APP_PKG` | `""` | Package name of preferred app (e.g. `com.spotify.music`) |
| `ENABLE_GPS_TIME_SYNC` | `true` | Enable auto-sync of system clock from GPS fix |

### How It Works

1. `BootReceiver` listens for `BOOT_COMPLETED` (and `HmiServiceReceiver` listens for `ACTION_HMI_SERVICE_START`) to launch `AutoOkService`.
2. `AutoOkService` initializes controllers (`CameraVolumeController`, `AudioMuteGuard`, `AutoAppLauncher`, `GpsTimeSync`).
3. `LogcatWatcher` executes `su -c "exec logcat -b events -v threadtime am_create_activity:I am_new_intent:I *:S"` on a background thread and streams activity transitions:
   - Matches disclaimer activity to trigger auto-tap.
   - Matches camera activity to duck audio stream.
   - Matches transition back from camera to restore audio stream.
4. `CameraBroadcastReceiver` receives camera broadcast intents (`EXIT_REAR_WIDE_CAMERA`, `ACTION_RESUME_CAMERA`, `DECREASE_VOLUME`, `RESTORE_VOLUME`) as a dual-path trigger.
5. `GpsTimeSync` requests GPS updates and corrects system time if satellite time diverges by > 5s.

---

## Compilation & Testing

Prerequisites: Android SDK (`compileSdk 33`), Java 17 or Java 21, Gradle 8.5.

```bash
cd our-apk
./gradlew test
./gradlew assembleRelease
```

All 7 unit test suites execute without an emulator or physical device in JVM:
- `AvisoMatcherTest`: Disclaimer needle and action matching
- `TapDeduperTest`: Deduplication window logic
- `CameraActivityMatcherTest`: Reverse camera activity transition parsing
- `CameraVolumeControllerTest`: Volume ducking calculation and boundaries
- `AudioMuteGuardTest`: Mute recovery threshold and camera exclusion logic
- `AutoAppLauncherTest`: App launch criteria and sanitization
- `GpsTimeSyncTest`: Clock drift calculation and timestamp formatting

Output APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (~16 KB).

---

## Installation via OTA Package (Recommended)

Copy the release APK to `tooling/ota-build/overlay/system/app/AutoOk.apk` and run:

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
- `AutoOk: watcher iniciado needle=...` — Service started with valid config.
- `AutoOk: tap ok=true coords=...` — Tap issued with exit status 0.
- `AutoOk: CameraVolumeController: ducked volume 15 -> 9` — Reverse camera duck active.
- `AutoOk: CameraVolumeController: restored volume 9 -> 15` — Reverse camera exited.
- `AutoOk: AudioMuteGuard: recovered stuck muted volume from 0 -> 5` — CarPlay mute bug auto-fixed.
- `AutoOk: GpsTimeSync: drift=12s -> setting system clock to ...` — Clock synchronized from GPS.

---

## Module Structure

```
our-apk/
├── build.gradle               (AGP 8.2.2)
├── settings.gradle
├── gradle.properties
├── README.md                   (this file)
└── app/
    ├── build.gradle           (minSdk/targetSdk 17, compileSdk 33)
    ├── proguard-rules.pro
    └── src/
        ├── test/java/com/khalleb/civichmi/autook/
        │   ├── AvisoMatcherTest.java
        │   ├── TapDeduperTest.java
        │   ├── CameraActivityMatcherTest.java
        │   ├── CameraVolumeControllerTest.java
        │   ├── AudioMuteGuardTest.java
        │   ├── AutoAppLauncherTest.java
        │   └── GpsTimeSyncTest.java
        └── main/
            ├── AndroidManifest.xml
            ├── res/values/strings.xml
            └── java/com/khalleb/civichmi/autook/
                ├── AutoOkConfig.java           (constants & configuration)
                ├── BootReceiver.java           (BOOT_COMPLETED listener)
                ├── HmiServiceReceiver.java     (ACTION_HMI_SERVICE_START listener)
                ├── CameraBroadcastReceiver.java(Camera intent relay)
                ├── AutoOkService.java          (main lifecycle coordinator)
                ├── LogcatWatcher.java          (logcat stream parser)
                ├── CameraActivityMatcher.java  (Camera transition matcher)
                ├── CameraVolumeController.java (Audio ducking & restoration)
                ├── AudioMuteGuard.java         (Stuck mute recovery daemon)
                ├── AutoAppLauncher.java        (Boot preferred app launcher)
                ├── GpsTimeSync.java            (GPS satellite time synchronizer)
                ├── TapDeduper.java             (Tap deduplication)
                ├── AvisoMatcher.java           (Disclaimer activity matcher)
                ├── Shell.java                  (su execution helper)
                └── FileLogger.java             (Persistent file logging)
```
