# Phase 10 — Permanent Warning Elimination (Pre-Planning)

> Comprehensive technical investigation conducted on 2026-10-20 from diagnostics
> collected via `build-diagnostic.py`. This document was the engineering briefing
> that prepared the definitive removal of the disclaimer.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../../docs/fase10-pre-planejamento-2026-10-20.md)

---

## Objective

Permanently eliminate the startup warning Activity (`OpeningDivideActivity`) from the head unit's boot flow. While AutoOk v0.5.4 automatically taps OK, the screen still flashes for ~2–5 seconds. Phase 10 removes the screen entirely: 0 visual display, 0 tap, 0 logcat watcher required.

---

## The Discovered Gate

`DaLauncher.apk` evaluates whether to display the warning screen via an in-memory `static boolean` named `sDisclamerFlg` within `OpeningDivideActivity`. It is **never persisted to disk** — resetting to `false` on every class load, which is why the disclaimer appears on every cold start.

Confirmed in deodexed smali (`tooling/apks-decoded/DaLauncher-deodexed/`):

```
OpeningDivideActivity.smali:17   .field public static sDisclamerFlg:Z
OpeningDivideActivity.smali:67   sput-boolean v0, ...sDisclamerFlg:Z    ← clinit, v0 = false
OpeningDivideActivity.smali:669  sget-boolean v0, ...sDisclamerFlg:Z    ← reads the flag
OpeningDivideActivity.smali:671  if-nez v0, :cond_9                      ← if true, skips disclaimer
DisclaimerFragment.smali:4363    sput-boolean v5, ...sDisclamerFlg:Z    ← sets true after tapping OK
HmiBroadcastReceiver.smali:492   sput-boolean v10, ...sDisclamerFlg:Z   ← Honda resets to false on LP0
HmiBroadcastReceiver.smali:636   sput-boolean v10, ...sDisclamerFlg:Z   ← secondary reset
```

The method `createDisclaimerOrEcoIntent()` in `OpeningDivideActivity.smali` (lines 664–688) controls the branch:

```smali
.method private createDisclaimerOrEcoIntent()Landroid/content/Intent;
    sget-boolean v0, ...sDisclamerFlg:Z
    if-nez v0, :cond_9
    invoke-direct {p0}, ...createDisclaimerOnHomeIntent()...    # shows disclaimer
    move-result-object v0
    :goto_8
    return-object v0
    :cond_9
    invoke-direct {p0}, ...createEcoIntent()...                 # proceeds to Eco screen
    move-result-object v0
    goto :goto_8
.end method
```

Equivalent Java representation:

```java
private Intent createDisclaimerOrEcoIntent() {
    if (sDisclamerFlg) {
        return createEcoIntent();              // standard next screen
    }
    return createDisclaimerOnHomeIntent();     // warning disclaimer screen
}
```

---

## The Smali Patch

**Single-instruction modification in `OpeningDivideActivity.smali` at line 671:**

```smali
# Original:
if-nez v0, :cond_9

# Patched:
goto :cond_9
```

This forces an unconditional branch to `createEcoIntent()`, ignoring `sDisclamerFlg` and treating the disclaimer as perpetually acknowledged.

---

## DaLauncher Signature & Packaging Strategy

`DaLauncher.apk` is originally signed with Mitsubishi corporate keys (`CN=Melco Honda16MY`).

Because we do not have the private Melco key, the APK is re-signed with the public **AOSP testkey**, which is explicitly trusted by the recovery update framework.

### Why Route B Works:
1. `DaLauncher` does not declare a `sharedUserId`, preventing UID verification breakage.
2. `VEHICLE_RW` permission maps to UNIX group `vehicle_rw` without signature checks.
3. `/system/vendor/app/DaLauncher.odex` is deleted during installation to prevent Dalvik bytecode cache desynchronization.
4. If reverting is ever required, the original `DaLauncher.apk` and `DaLauncher.odex` are fully restored via `build-dalauncher-restore.py`.
