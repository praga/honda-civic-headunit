# AutoOk.apk — Technical Details

Architecture, design decisions, resolved challenges, and configuration for the autonomous auto-tap APK.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../../docs/fase7-deploy-2026-10-16/04-apk-autook-detalhes.md)

---

## Core Purpose

Once installed in `/system/app/` with setuid `su` in `/system/xbin/`, manual taps on the disclaimer OK button become unnecessary. Whenever the head unit displays the startup Warning screen:

1. Detects via logcat that `OpeningDivideActivity` has been brought to the foreground.
2. Waits 2 seconds for the Fragment and UI layout to finish rendering.
3. Dispatches `su -c "input tap 400 350"` — programmatically tapping OK.
4. Warning screen dismisses.
5. Re-arms watcher thread for the next power cycle.

---

## Component Architecture

```
AutoOkConfig       # Constants (Activity names, screen coords, timeout delays)
    ↓
BootReceiver       # Receives BOOT_COMPLETED & ACTION_HMI_SERVICE_START → starts Service
    ↓
AutoOkService      # Persistent Service orchestrating LogcatWatcher & TapDeduper
    ↓
LogcatWatcher      # Worker thread reading `su -c "logcat -v brief ActivityManager:I *:S"`
    ↓
Shell              # Static helper executing root shell commands
```

### `AutoOkConfig.java` Settings

```java
package com.khalleb.civichmi.autook;

public final class AutoOkConfig {
    public static final String AVISO_ACTIVITY_NAME =
        "com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity";

    public static final int BUTTON_OK_X = 400;
    public static final int BUTTON_OK_Y = 350;

    public static final int DELAY_SECONDS_AFTER_DETECT = 2;
    public static final int REARM_DELAY_SECONDS = 10;
    public static final int BOOT_INITIAL_WAIT_MS = 5000;
    public static final String LOGCAT_TAG = "AutoOk";
}
```

---

## Technical Challenges & Solutions

### 1. Logcat Needle Matching
ActivityManager logcat entries format component identifiers as `pkg/.ClassName` (with a forward slash followed by a period). Searching for `com.mitsubishielectric.ada.app.dalauncher.OpeningDivideActivity` would fail to match. The exact needle is `com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity`.

### 2. D8 Compiler & Anonymous Classes
Older D8 toolchains targeting API 17 Jelly Bean produced `NullPointerException` when processing anonymous inner classes within static listener callbacks. Refactoring to explicit named inner classes resolved compiler crashes.

### 3. Service Persistence Across Suspend (LP0)
In standard Android behavior, background services terminating with `stopSelf()` stay terminated until reboot. Because Honda vehicles enter LP0 suspend without triggering full cold reboots on ignition toggle, `BOOT_COMPLETED` does not fire. `AutoOk` implements `rearmWatcher()` to remain resident in memory indefinitely.
