---
name: "🐛 Bug report"
about: Report an issue or unexpected behavior with the head unit tooling
title: "[BUG] "
labels: bug
assignees: ''
---

## Description

<!-- Describe clearly what is happening -->

## Expected Behavior

<!-- What should happen instead? -->

## Steps to Reproduce

1. 
2. 
3. 

## Environment

- **Head Unit Firmware / Build (`ro.build.id`):** (e.g. `1.F187.16`)
- **ROM Type (`custom_rom.type`):** (e.g. `0040`)
- **PC Operating System:** (e.g. Linux Fedora 44, Ubuntu 24.04, macOS)
- **Python Version:**
- **Java Version:**
- **Android SDK build-tools Version:**

## Logs

### `install-0040.log` or `rollback-0040.log` from USB drive

```
paste here
```

### `result.log` from recovery

```
paste here
```

### Relevant Logcat (if APK is installed)

Extracted via Service Menu → `Detail Information & Setting` → `Error History` → `Save` (Hold `DAY/NIGHT + AUDIO + MENU` for 2s with audio off).

Suggested grep:

```bash
grep -iE "AutoOk|daupdater|SwUpdate|OpeningDivide" logcat*
```

```
paste here
```

## Screenshots / Photos (Optional)

<!-- Attach any photos of the head unit screen or laptop console output -->
