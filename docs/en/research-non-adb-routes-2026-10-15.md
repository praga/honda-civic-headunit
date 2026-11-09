# Survey — Deploying AutoOk Without ADB (2026-10-15)

Survey of alternative routes evaluated after discovering that ADB developer mode was blocked by a passcord on Brazilian firmware `1.F187.16`.

*Language:* 🇺🇸 **English** · [🇧🇷 Português](../pesquisa-rotas-sem-adb-2026-10-15.md)

---

## 1. Summary

When direct ADB activation over USB failed, we conducted a global reverse engineering survey to identify non-ADB deployment vectors. This led directly to selecting **EvilValet (Route B)** — leveraging public AOSP testkey verification flaws in the recovery partition.

---

## 2. Evaluated Routes Summary

| Route | Vector | Feasibility | Decision |
|---|---|---|---|
| **Route A** | USB-A/A hardware cable + host switch | Blocked without ADB enabled in OS | Discarded |
| **Route B** | **EvilValet (AOSP testkey via USB OTA)** | **High — verified in US & EU firmwares** | **Selected & Implemented** |
| **Route C** | Public Honda OTA update extraction | Medium — requires finding exact matching dump | Preserved for reference |
| **Route D** | Hidden physical combo for DeveloperDiag | Low — DeveloperDiag still restricts ADB toggle | Discarded |
| **Route E** | Browser exploit (WebKit dirtyCOW) | Low — requires online staging & vulnerable engine | Discarded |
| **Route F** | Direct eMMC hardware tapping / JTAG | High technical barrier & invasive hardware risk | Discarded |
| **Route G** | Dealer firmware downloader portal | Useful for obtaining official firmware binaries | Supplementary |

---

## 3. Why EvilValet (Route B) Won

1. **Non-invasive:** Requires only a standard FAT32 USB flash drive plugged into the factory front USB port.
2. **Zero dependencies on OS UI:** Bypasses Android userland passcord restrictions completely by acting in recovery.
3. **Full root execution:** Runs as root within the recovery environment, enabling arbitrary `/system` read/write.
4. **Reversible:** Clean rollback packages can be prepared beforehand using the exact same mechanism.
