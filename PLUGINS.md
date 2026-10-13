# BoxTune Lite 2.2 — Community Plug-in Specification

## What the importer actually does

BoxTune opens a user-selected local `.json` manifest using Android's document picker. The importer verifies the schema and supported category/action, shows the manifest in the app, and requires **Approve and Install** before saving it privately. Running the installed plugin invokes a prebuilt, allowlisted action. It **does not execute JavaScript, Java, shell code, APK files, download executable plugins, install packages, or grant root access**. Community plugins are untrusted, unsigned local data; do not treat them as verified developer releases.

You can place these `.json` files on a USB drive or in Downloads, and community members can share their own manifests. At most **20 community plugins** are stored; 10 sample plugins are included. There is a two-step **Remove All Community Plugins** action. All effects require tapping the plugin button. Hardware and firmware support still determine which Android settings screens are available.

## Format (schema 1)

```json
{
  "schema": 1,
  "id": "my-speed-panel",
  "name": "My Mouse Settings",
  "category": "System",
  "action": "mouse",
  "description": "Opens supported pointer settings and asks for access if needed."
}
```

Fields:

- `schema`: exactly integer `1`.
- `id`: 1–40 lowercase alphanumeric characters, dots, hyphens, or underscores.
- `name`: 1–64 characters.
- `description`: up to 160 characters.
- `category`: exactly `System` or `App` (case-sensitive).
- `action`: one of the category-specific items below.
- Maximum file size: about 12 KiB.

### System actions (5 built-in, safe)

| Action | Effect |
| --- | --- |
| `storage` | Open Android internal-storage settings |
| `apps` | Open app-management settings |
| `display` | Open Android display settings |
| `network` | Open Android connection settings |
| `mouse` | Open Modify System Settings access for optional pointer-speed control |

### App actions (5 built-in)

| Action | Effect |
| --- | --- |
| `dark` | Toggle BoxTune app dark/light appearance |
| `compact` | Toggle compact keyboard/mouse layout in BoxTune |
| `diagnostics` | Refresh live device and network information |
| `scanner` | Show the user-controlled unused-app review; Usage Access must be granted separately |
| `roblox` | Launch Roblox if the app is installed |

## Security and practical limitations

- No arbitrary commands, third-party executable code, internet downloads, or hidden privilege changes.
- Community imports can only reuse the **10 actions already programmed into BoxTune**. They cannot invent new low-level optimizations without a future trusted APK update.
- `System` plugins do **not** become Android system apps. System settings access remains subject to Android permissions and manufacturer firmware.
- The imported plugin is shown for approval first; no app uninstall runs silently.
- If the Android TV box has no document picker, you may need a compatible file-manager app.
- The `roblox` action only launches the app; it does not improve GPU or RAM speed. Network controls likewise cannot promise lower ping.
- App version 2.2 is compiled for Android API 26+ (Android 8.0). APK structural validation and signature checks cannot replace tests on real devices.

## Import from a community member

1. Download a trusted-looking `.json` plugin from your community and inspect its contents. It should contain *data only*, like the example above.
2. Copy it to the TV box (e.g. using USB drive), open BoxTune → **IMPORT .JSON COMMUNITY PLUGIN**, and select the file.
3. Read its proposed name, category, and exact action before choosing **APPROVE AND INSTALL**.
4. Its button appears under **IMPORTED COMMUNITY PLUGINS**; tap to perform the action.
5. Remove all community plugins through the two-step removal control if you no longer want them.

The project source and the 10 sample plugin files can be modified and shared freely. Do not distribute or install third-party repackaged BoxTune APKs claiming to contain extra optimizer powers without reviewing their permissions and signatures.

## GitHub catalog in v2.3

`plugins/index.json` contains each manifest's schema, id, name, description, category, action, integer `version`, and relative `path` (e.g. `system/storage-sweep.json`). It must match the downloaded file fields.

The app fetches the catalog on launch and on Refresh; new plugin listings do **not** auto-install, since system access and community code must remain opt-in. Existing id upgrades require the same category and action and additional user approval.

The app allows at most 20 community plugins. Plugin actions still only open Android screens or change BoxTune-local preferences.
