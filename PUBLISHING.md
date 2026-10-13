# How to publish BoxTune Lite to GitHub

Owner: `drivebackupharuka-ops` (connected account). Repository name: `BoxTune-Lite`.

## Publish the existing repository

The public empty GitHub repository `drivebackupharuka-ops/BoxTune-Lite` exists. The ChatGPT GitHub connector returned HTTP 403 `Resource not accessible by integration` on attempting to write README.md, and reported zero authorized installations. Grant the GitHub App access to the repository in GitHub **Settings → Applications → Installed GitHub Apps** (if available) or reconnect GitHub in ChatGPT **Settings → Plugins**, selecting `BoxTune-Lite` among permitted repositories. Once authorized, ChatGPT can publish the files.

To publish manually, extract the ZIP and, from the `BoxTune-Lite/` folder with Git installed, run:

```sh
git init
git branch -M main
git add .
git commit -m "Initial BoxTune Lite 2.3, plugin catalog, 10 plugins"
git remote add origin https://github.com/drivebackupharuka-ops/BoxTune-Lite.git
git push -u origin main
```

Never commit passwords, Android signing keystores, private keys or personal tokens.

## What automatically updates?

- Adding/updating any plugin manifest in `plugins/system/` or `plugins/app/` triggers the GitHub `Update plugin catalog` workflow, which rebuilds `plugins/index.json` on `main`.
- The BoxTune 2.3 APK checks the public GitHub catalog at launch and has a `REFRESH GITHUB PLUGINS` button. The list changes without an APK update.
- The app downloads JSON plugins only after a tap, previews them and asks for installation approval. Auto-installing unreviewed community plugins is intentionally unsupported.
- GitHub Actions also offers a debug APK build for application code changes. Debug builds are not signed with the private release key and cannot seamlessly upgrade the included release APK. Plugin-only changes do not rebuild the APK.

## Permissions and limitations

Your repository must be **public** for anonymous RAW HTTPS access. The app does not use a GitHub token or require anyone to log in. If the GitHub Actions workflow can't push its index update, go to repository Settings → Actions → General → Workflow permissions and allow GitHub Actions to write repository contents, or regenerate the catalog locally and commit it manually. This is a repository setting, not Android administrator/root access.

No APK has been run on physical TV hardware in this environment. The included v2.3 APK passed package/signature and structural bytecode checks, which cannot guarantee that custom Android TV firmware will run it successfully.
