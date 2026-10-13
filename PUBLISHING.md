# BoxTune Lite publishing guide

Repository: https://github.com/drivebackupharuka-ops/BoxTune-Lite

## Downloading

The current signed APK is available from [GitHub Releases](https://github.com/drivebackupharuka-ops/BoxTune-Lite/releases/latest). Open **Assets** and download the `.apk` file. SHA-256 checksum assets are published alongside APKs.

## Publishing a new APK version

1. Update the Android source and the application version in `app/build.gradle`. Build and test on a supported device.
2. **Sign the release APK with the same private signing key as the previously distributed version.** Android will reject an in-place update with a different certificate. Keep private keystores and passwords out of this public repository.
3. Commit the signed APK at `dist/BoxTuneLite-X.Y-android8.apk` on the `main` branch, using a higher numeric version such as 2.4.
4. The `Publish BoxTune APK Release` workflow in `.github/workflows/release.yml` will run automatically when that file is added or changed. It scans `dist/`, selects the highest versioned APK, and publishes it as tag `vX.Y` with a corresponding SHA-256 checksum.
5. Check [Actions](https://github.com/drivebackupharuka-ops/BoxTune-Lite/actions/workflows/release.yml) for a successful run, then verify both files appear under [Releases](https://github.com/drivebackupharuka-ops/BoxTune-Lite/releases).

You can rerun the workflow via **Actions → Publish BoxTune APK Release → Run workflow**. If the release already exists, it refreshes the APK, checksum, and release notes.

**Important:** This workflow *publishes* the signed APK already stored in `dist/`. It does not build or securely sign an APK itself. The separate `.github/workflows/android.yml` is a development build and produces a debug-signed APK that is not interchangeable with the existing signing certificate.

If publishing fails with 403 `Resource not accessible by integration`, check repository **Settings → Actions → General → Workflow permissions** for the ability to write repository contents. The workflow requests `contents: write`. Repository or organization policies may still override this.

## Plugin updates

Plugin manifests live under `plugins/system/` and `plugins/app/`. The `Update plugin catalog` workflow automatically rebuilds `plugins/index.json` when manifests change. BoxTune 2.3 fetches the public raw catalog when opened or refreshed. Downloads and installations require user approval, and the JSON plugins cannot execute arbitrary code. Plugin-only changes do not require a new Android APK release.

## Safety

No app version has been physically tested on the user's specific TV box here. The APK was signed and statically checked offline. Never publish Android signing keys, GitHub tokens, or passwords, and never promise CPU overclocking, zero input lag, or automatic Android root privileges.
