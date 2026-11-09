# Scan 9 — Camera (Layer 2: AIDL + Broadcasts + CAN Signal)

**Date:** 2026-10-31  
**Methodology:** Deodexing of `Camera.apk` + `CameraAPService.apk` + `CameraAPServiceLib.jar` (baksmali 2.5.2, API 17, complete bootclasspath), reading generated smali files (384 + 92 + 127 files), `aapt` inspection of binary manifests, cross-grepping.  
**Objective:** Determine whether Phase 9 (AutoOk reverse detection without logcat polling) has a viable native platform API.

This scan complements section §11.4 of the MAP — which only documented the Activity from the watcher's needle perspective — with the complete IPC architecture: Binder interfaces, listeners, broadcasts, CAN bus signals, and Activity launching. Links with [`07-services-re.md`](07-services-re.md) (VehicleInfoManager + CAN) and [`01-apis-aidl.md`](01-apis-aidl.md) (AIDL catalog).

---

## 1. Trust & Manifest

### 1.1 CameraAPService (`vendor-app/CameraAPService.apk`)

```
package           com.mitsubishielectric.ada.appservice.camera
service           CameraAPService
  exported        true
  intent-filter   action com.mitsubishielectric.ada.appservice.camera.ICameraAPService
sharedUserId      —   (none)
uses-permission   com.mitsubishielectric.ada.permission.VEHICLE_RW
                  android.permission.WRITE_EXTERNAL_STORAGE
                  android.permission.WRITE_MEDIA_STORAGE
                  android.permission.ACCESS_ALL_EXTERNAL_STORAGE
uses-library      cpucomservice, vehicledbmanager, appmanagerservice, avapservice,
                  unitinfomanager, vehicleinfomanager, vehiclecoordination,
                  cameraapservice, systeminfoservice, videomanager (all platform framework libs)
```

### 1.2 Camera App (`vendor-app/Camera.apk`)

```
package           com.mitsubishielectric.ada.app.camera
sharedUserId      —   (none)
uses-permission   com.mitsubishielectric.ada.permission.VEHICLE_RW
                  android.permission.CAMERA
                  android.permission.VIBRATE
                  android.permission.STATUS_BAR_SERVICE
                  android.permission.SYSTEM_ALERT_WINDOW
                  android.permission.INTERNAL_SYSTEM_WINDOW
                  android.permission.CHANGE_CONFIGURATION
activity          .activity.RearWideCameraDisplayActivity (dedicated taskAffinity)
                  .activity.RearWideCameraGuideLineAdjustActivity
                  .activity.RearWideCameraSettingActivity
                  .activity.SteerAngleAdjustmentResultActivity
                  .activity.CornerCameraDisplayActivity + {View,Setting,GuideLineAdjust}
                  .activity.FrontCameraDisplayActivity + .FrontCameraSettingActivity
                  .activity.LaneWatchDisplayActivity + {Caution,Setting}
                  .activity.MvcDisplayActivity + {Setting,CtmSetting}
                  .activity.CameraSettingActivity, DisplaySettingActivity,
                  BsiCtmSettingActivity, CameraColdBootActivity
receiver          .util.BroadcastReceiverForCTS
                  (intent-filter: android.media.action.IMAGE_CAPTURE / VIDEO_CAPTURE)
```

No Activity in Camera app has an `intent-filter` matching `com.mitsubishielectric.android.action.camera.*`. They are launched via `IAppManagerService.startActivity(Intent, int)` with explicit class names (see §5).

### 1.3 Permission `VEHICLE_RW` — Critical Factor

Declared in `vendor-app/VehicleRW.apk`:

```xml
<permission android:name="com.mitsubishielectric.ada.permission.VEHICLE_RW"
            android:label="vehicle data access"
            android:protectionLevel="0x3" />
```

`protectionLevel=0x3` represents `signatureOrSystem`. Any APK residing in `/system/app/` or `/system/vendor/app/` is eligible automatically regardless of signature — requiring only `<uses-permission>`. This ensures the viability of Option A in §6 (AutoOk already resides in `/system/app/` and can claim `VEHICLE_RW` merely by adding the tag).

---

## 2. AIDL — `ICameraAPService` (164 Transactions)

Interface `Lcom/mitsubishielectric/ada/appservice/camera/ICameraAPService;` inside `CameraAPServiceLib.jar`. 164 `TRANSACTION_*` constants in `$Stub`, grouped by function:

### 2.1 Listener Management (12 register/unregister pairs)

| Listener | Register TX | Unregister TX | Interface |
|---|---|---|---|
| ReverseSignal 🎯 | `0x57` | `0x58` | `ICameraAPServiceReverseSignalListener` |
| BindComplete | `0x5b` | `0x5c` | `ICameraAPServiceBindCompleteListener` |
| SetViewMode (Result) | `0x5e` | `0x5f` | `ICameraAPServiceSetViewModeResultListener` |
| ChangeViewMode | `0x6c` | `0x6d` | `ICameraAPServiceChangeViewModeListener` |
| TransparentRequest | `0x6e` | `0x6f` | `ITransparentRequestListener` |
| IgState | `0x6a` | `0x6b` | `ICameraAPServiceIgOnStateListener` |
| DrivingOperationStatus | `0x4c` | `0x4d` | `ICameraAPServiceDrivingOperationStatusListener` |
| DrivingStatus | `0x50` | `0x51` | `ICameraAPServiceDrivingStatusListener` |
| LaneWatch | `0x28` / `0x61` | `0x29` / `0x62` | `ICameraAPServiceLaneWatchListener`, `*ComunicateListener` |
| MVC | `0x85` / `0x80` / `0x87` | `0x86` / `0x81` / `0x88` | `ICameraAPServiceMvcListener`, `*ComunicateListener`, `*CameraStatusListener` |
| BsiCtm | `0x44` | `0x45` | `ICameraAPServiceBsiCtmListener` |
| Errors & Guidelines | `set*` / `deset*` (0x10–0x1a) | same name | `ICameraErrorListener`, `ICameraAPServiceCommonListener`, `IUpdateGuidelineListener`, `IBsiCtmListener`, `IParkingSensorListener`, `ICtmListener` |

### 2.2 Synchronous State Queries

- Camera state / reverse gear: `getCameraState`, `isCameraFinished`, `getCameraDestination`.
- Reverse-specific: `getRearWideCameraGuidelineAdjust`, `getRearWideCameraFixGuidelinePoint`, `getRearWideCameraCautionAdjust`, `isRearWideCameraLoadFileExists`.
- MVC: `getMvcDisplayStateRequest`, `getMvcLastDisplayMode`, `getMVCMoveDirection`, `getMVCParkingAssist`, `getMemoryLastScreenModeSetting`.
- Vehicle dynamics: `getSteeringAngle`, `getMaxSteerAngle`, `getAngleSensor`, `getIgStatus`, `getIgOnAfterPowerOn`, `getDrivingOperationStatus`, `getDrivingStatus`, `getChangeSpeedReference`, `getHandlePosision` (sic).
- Diagnostic / Debug: `isDiagMode`, `isDisclaimerScreenPassed` 🎯, `isParkingAssist`, `isShowBrightnessBar`, `isStrageDeviceExists` (sic).

### 2.3 Commands & Control

- Launch camera: `startCameraCommonApplication` (0x02), `startCameraActivity` (0x9b), `exitCameraCommonApplication` (0x03), `finishCameraCommonApplication` (0x42).
- Direct reverse control: `startReverseStateWatcherRequest` (0x7f), `setRearWideCameraPowerStatus` (0x99), `endEarlyRvcPreview` (0x5a), `notifyEarlyRvcPreviewStatus` (0x59).
- Camera hardware power: `cameraPowerOn` (0x39), `cameraPowerOff` (0x3a).

The primary mechanism for reverse detection is the pair **`registReverseSignalStatusListener` / `unregistReverseSignalStatusListener`** (TX `0x57`/`0x58`).

---

## 3. Asynchronous Callback Listeners

All defined in `CameraAPServiceLib.jar` as interface + `$Stub` + `$Stub$Proxy`:

| Interface | Callback(s) | Event Description |
|---|---|---|
| `ICameraAPServiceReverseSignalListener` 🎯 | `onNotifyReverseSignalStatus(int)` | `0` = exited reverse; `1` = engaged reverse |
| `ICameraAPServiceCommonListener` | `onDisplayModeChanged(int)` | Display mode changes (day / night) |
| `ICameraAPServiceChangeViewModeListener` | `onNotifyChangeViewMode(int)` | Camera view mode adjustments |
| `ICameraAPServiceSetViewModeResultListener` | `onNotifyChangeSetViewModeResult(int)` | Acknowledgment of `setViewMode` |
| `ICameraAPServiceBindCompleteListener` | `onBindComplete(int)` | Service initialization binding completed |
| `ICameraAPServiceIgOnStateListener` | `onNotifyIgState(int)` | Ignition status changes |
| `ICameraAPServiceDrivingStatusListener` | `onNotifyDrivingStatus(int)` | Vehicle moving vs stationary |
| `ICameraAPServiceLaneWatchListener` | `onAimingState`, `onBcanState`, `onConnectState`, etc. | LaneWatch right-turn camera |
| `ICameraAPServiceBsiCtmListener` | `onNotifyBcanInformation(int, Bundle, int)` | Raw B-CAN vehicle packet feed |

---

## 4. Broadcasts (`com.mitsubishielectric.android.action.camera.*`)

| Action | Broadcast By | Consumed By |
|---|---|---|
| `EXIT_REAR_WIDE_CAMERA` 🎯 | CameraAPService (`RearWideCameraManager.finishApp`) & Camera.apk | **No component in the shipping ROM** (broadcasts into void) |
| `EXIT_REAR_WIDE_CAMERA_SET` | CameraAPService | Camera.apk (runtime receiver) |
| `EXIT_FRONT_CAMERA`, `EXIT_FRONT_CAMERA_SET` | CameraAPService + Camera.apk | Internal camera state machine |
| `EXIT_CORNER_CAMERA`, `EXIT_CORNER_CAMERA_SET` | CameraAPService + Camera.apk | Internal camera state machine |
| `EXIT_MVC`, `EXIT_MVC_SET` | CameraAPService + Camera.apk | Multi-View camera |
| `EXIT_LANEWATCH` | CameraAPService + Camera.apk | LaneWatch camera |

**Crucial finding:** There is NO `ENTER_REAR_WIDE_CAMERA` broadcast. The platform does not broadcast reverse engagement; it only broadcasts exit (`EXIT_REAR_WIDE_CAMERA`). Reverse engagement is communicated exclusively via:
1. **AIDL**: `ICameraAPServiceReverseSignalListener.onNotifyReverseSignalStatus(1)`.
2. **Activity Launch**: `IAppManagerService.startActivity(Intent{RearWideCameraDisplayActivity}, 4)`.

---

## 5. End-to-End Reverse Flow

### 5.1 Architecture Pipeline

```
B-CAN transmits CVT_SHFT_IN_REVERSE
          ↓
VehicleInfoManagerApService (bound by CameraAPService)
          ↓
IVehicleCanInformationForCameraApServiceListener
          ↓
CameraAPService → ReverseStateWatcher.getReverseStatus() → RearWideCameraManager.reverseChanged(int)
          ↓
          ├─→ Engaged Reverse (p1 == 1):
          │     • startCameraCommonApplicationImpl(StartMode.RVC, …)
          │     • Intent setClassName("com.mitsubishielectric.ada.app.camera",
          │                           ".activity.RearWideCameraDisplayActivity")
          │     • IAppManagerService.startActivity(intent, 4)   ← Camera display pops up
          │     • RemoteCallbackList.broadcast() → onNotifyReverseSignalStatus(1)
          │
          └─→ Exited Reverse (p1 == 0):
                • finishApp() → sendBroadcast("com.mitsubishielectric.android.action.camera.EXIT_REAR_WIDE_CAMERA")
                • RemoteCallbackList.broadcast() → onNotifyReverseSignalStatus(0)
                • (Camera Activity pops down on receipt of EXIT_REAR_WIDE_CAMERA)
```

---

## 6. Phase 9 Assessment & Recommendations

Goal: Transition AutoOk away from logcat polling for reverse gear detection.

### Option A — Direct Bind to `ICameraAPService` + Register `ReverseSignalListener`
- **Pros:** Zero polling, sub-millisecond response, captures both entry and exit.
- **Cons:** Requires compile-time stubs from `CameraAPServiceLib.jar`, lifecycle handling across LP0 sleep/wake, and mock-heavy testing.

### Option B — `BroadcastReceiver` for `EXIT_REAR_WIDE_CAMERA`
- **Pros:** Extremely low friction, zero permissions required, no complex AIDL binding.
- **Cons:** Only signals exit; entry still requires Activity monitoring or AIDL.

### Option C — Maintain Current Baseline (Events Logcat Polling)
- **Status:** AutoOk v0.5.4 has been running with 100% stability in production. Following Phase 10's smali patch, AutoOk acts purely as a fallback safety net.

### Recommendation
Maintain **Option C** for current stability, with opportunistic adoption of **Option B** whenever the next minor APK rebuild occurs.
