# BoxTune Lite — Android TV Box utility

BoxTune Lite 2.3 is a lightweight Android 8+ TV-box utility with hardware diagnostics, basic display/input shortcuts, review-first maintenance, and a GitHub JSON plugin catalog.

The remote catalog is `plugins/index.json`; plugin manifests live under `plugins/system/` and `plugins/app/`. The application refreshes the catalog on launch or on demand, but every plugin installation requires review and approval. Plugins are data-only and never execute arbitrary code.

No root, CPU overclocking, guaranteed FPS boosts, silent uninstalls, or unrestricted system modifications.

Build source: `app/` with Gradle. Plugins: `plugins/`. See project files and workflows for details.

**Hardware notice:** APK was statically checked and signed, but it has not been tested on this particular Android TV box.