# BoxTune Lite 2.3 — GitHub Plugin Catalog

Lightweight Android 8+ (API 26+) TV-box utility for diagnostics, simple display/mouse settings and permission-first maintenance. **No root or performance guarantees.**

## GitHub plugins

`plugins/index.json` is a schema-v1 catalog of JSON-only plugin manifests in `plugins/system/` and `plugins/app/`. The Android app checks the catalog when launched, also offering a manual refresh and a last-known-good cached listing. It then downloads selected manifests from:

`https://raw.githubusercontent.com/drivebackupharuka-ops/BoxTune-Lite/main/plugins/`

When you add or edit a manifest in the `main` branch and update its entry and `version` in `plugins/index.json`, users see the new listing the next time their BoxTune app refreshes. New plugins and updates **always require review and approval**; changes are never silently installed. No arbitrary code is executed; the app recognizes only the ten hardcoded, limited actions in PLUGINS.md. A plugin cannot increase CPU/GPU performance or root the box.

Project files are published in this repository.

## Plugin update process

1. Add or modify `plugins/system/<id>.json` or `plugins/app/<id>.json`.
2. Increment `version` (integer) in the changed manifest; keep its id and action stable for updates.
3. Commit to `main`. `.github/workflows/plugin-catalog.yml` validates the JSON and automatically regenerates `plugins/index.json` in a follow-up commit. Alternatively, run `python scripts/generate_catalog.py` yourself before committing.
4. Refresh/reopen BoxTune. The app fetches new catalog listings on launch; GitHub RAW may cache briefly.
5. Review/approve a new or changed plugin on the TV box. Nothing installs silently.

## Build/install

The GitHub Actions workflow (`.github/workflows/android.yml`) builds a **debug APK** on changes to app code. Plugin JSON changes don't require an APK rebuild. GitHub CI builds are signed with a GitHub runner debug key, **different from the certificate of the included signed BoxTune 2.3 APK**: uninstalling the previous app may be necessary to install the debug build, which erases app preferences. Never upload the private 2.2 signing keystore to a public repository; for seamless in-place upgrade, sign a release with the original private key via a trusted local build or configured GitHub Secrets.

With Android Studio: use Android Gradle Plugin 8.7.3, Gradle 8.10.2, Java 17 and SDK 35. Run `gradle :app:assembleDebug`.

## Security

- GitHub HTTPS origin is pinned to `raw.githubusercontent.com` for JSON files. Redirects are refused, responses are bounded (64 KiB catalog, 12 KiB plugin), and the paths and action categories are validated.
- Manifest index metadata is advisory, not executable. Downloaded manifests must match index id, version, action and category. Full cryptographic publisher signing is **not** present; only use a trusted GitHub repository.
- User separately approves installing each new or updated plugin. No system privilege escalation; no silent package removal.
- No periodic background network work: refresh occurs on launch or by pressing a button. Works offline with a previously cached catalog.

## Files

- `app/src/main/java/` — Android source, including HTTPS catalog UI
- `plugins/index.json` — dynamic catalog for the TV app
- `plugins/system/` — 5 built-in System plugin manifests
- `plugins/app/` — 5 built-in App plugin manifests
- `PLUGINS.md` — JSON schema/action allowlist
- `.github/workflows/android.yml` — debug Android APK CI build
- `dist/BoxTuneLite-2.3-android8.apk` — 2.3 APK, offline-built and signed with the 2.2 certificate; static checks passed, not tested on a TV device
- `dist/BoxTuneLite-2.2-android8.apk` — prior version for reference
