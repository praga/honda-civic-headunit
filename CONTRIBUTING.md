# Contributing

🇺🇸 Thanks for your interest in contributing! · 🇧🇷 Obrigado pelo interesse em contribuir!

## Ways to Contribute

### 🚗 Report a New Honda Firmware Variant

**THE MOST VALUABLE CONTRIBUTION.** The more firmware revisions documented, the more universally useful this project becomes. Use the [`nova-safra.md`](.github/ISSUE_TEMPLATE/nova-safra.md) template.

Even if you haven't attempted to deploy — basic technical data (DA version, `custom_rom.type`, device model, screen resolution, cold boot key combination) helps construct a comprehensive compatibility matrix.

### 🐛 Bug Reports

Please use the [`bug-report.md`](.github/ISSUE_TEMPLATE/bug-report.md) template. Logs are vital — without logcat or deployment output, issues are difficult to diagnose.

### 🌍 Translations

- 🇺🇸 English: `docs/en/` (primary documentation)
- 🇧🇷 Portuguese: `docs/pt-BR/` (original source research)
- Other languages: open a pull request with a new `docs/<lang>/` folder.

### 💡 Technical Enhancements & Next Steps

Living roadmap: [`ROADMAP.md`](ROADMAP.md). Platform discoveries index (AIDL interfaces, broadcasts, trust map) in [`DISCOVERIES.md`](DISCOVERIES.md).

Current roadmap candidates:
- **Phase 9**: Native reverse camera detection via `ICameraAPServiceReverseSignalListener` (replaces logcat polling).
- **Validation**: Confirm `DISCLAIMER_COMPLETE` as the canonical dismissal signal across all states.
- **Whitelist Carrier**: Dump binary `com.honda.displayaudio.whitelist.apk`.
- **Property testing**: Evaluate `persist.sys.keyoff_reboot=1` to simplify power states.

### 📊 Dumps from Other Trims/Firmware

If you have access to a Honda head unit with a different firmware build, create a dump using our diagnostic/deep-scan package.

> **DO NOT upload proprietary firmware dumps directly to GitHub.** You may share sanitized extracts (e.g. system properties, package lists, decompiled structural definitions) in issues.

---

## Code Standards

### Commits
- Clear, descriptive messages.
- Imperative mood ("add X", "fix Y", "update Z").

### Pull Requests
- Descriptive branch naming: `feat/<name>`, `fix/<name>`, `docs/<name>`.
- Explain both **what** changed and **why**.
- Keep documentation updated alongside any code changes.

### Python
- Python 3.10+ compatible.
- Tested via `python3 -m unittest discover -s tooling/ota-build/tests`.

### Java (AutoOk APK)
- Java 1.8 target, Android API 17 (Android 4.2.2 compatibility).
- Use named inner classes (anonymous inner classes can crash legacy D8 toolchains).

---

## Code of Conduct

Please be respectful and constructive. We are a specialized open-source research community passionate about exploring and improving automotive head units.

## License

Code contributions are licensed under the [MIT License](LICENSE). Documentation is provided under CC BY 4.0.
