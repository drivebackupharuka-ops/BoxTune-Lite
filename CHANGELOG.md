# Changelog

## 2.3 GitHub Edition (source release)
- Remote HTTPS JSON catalog fetched on launch or manually; last-good local cache.
- In-app categorized listings from `plugins/index.json` with review/download prompts.
- Per-plugin version updates; reapproval required; same allowlisted category/action.
- GitHub Actions debug APK build workflow; no signing keys in repository.
- `INTERNET` permission added for explicit catalog retrieval.

# Changelog

## 2.2 — Community Plugins
- Added local JSON plugin importer with explicit preview and approve/reject actions.
- Introduced separate System and App plugin catalogs, five sample plugins in each category.
- Imported plugins persist privately across restarts and appear as buttons with a capped (20) catalog.
- Added action/category allowlisting, schema validation, a roughly 12 KiB input cap and two-step removal of community plugins.
- Added safe error handling for invalid JSON and unreadable documents in the APK parser.
- Preserved Android 8+ support, original app package name, and previous signing certificate.
- No executable plugin loading, root, device administration, remote downloads, or background execution.

## 2.1 — Maintenance & diagnostics
- Unused app review with Usage Access, optional uninstall confirmation.
- Dark-mode toggle and enhanced network diagnostics.
- Input, display, Bluetooth, pointer controls and companion-app shortcuts.

## 2.0 — Keyboard and mouse
- Compact mouse layout, mouse pointer presets and additional hardware diagnostics.
