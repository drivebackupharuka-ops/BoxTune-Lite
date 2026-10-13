package com.boxtune.lite;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * BoxTune Lite 2.3 — GitHub-connected, permission-first Android TV utility.
 * Built without external analytics, background services, root, device-admin,
 * privileged APIs, covert package operations or downloaded executable plugins.
 * Usage access and WRITE_SETTINGS are requested via Android settings screens.
 */
public final class BoxTuneActivity extends Activity implements View.OnClickListener {
    private TextView status;
    private boolean desktopMode = false;
    private boolean darkMode = true;
    private boolean scanMode = false;
    private boolean confirmClear = false;
    private static final int PICK_PLUGIN = 220;
    private static final String PLUGIN_PREFS = "boxtune.community.plugins";
    private static final String REPOSITORY = "drivebackupharuka-ops/BoxTune-Lite";
    // Public, JSON-only repository. New system actions still require a signed APK update.
    private static final String RAW_BASE = "https://raw.githubusercontent.com/" + REPOSITORY + "/main/plugins/";
    private static final String CATALOG_URL = RAW_BASE + "index.json";
    private static final int CATALOG_LIMIT = 64 * 1024;
    private static final int PLUGIN_LIMIT = 12 * 1024;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean fetching = false;
    private String catalogMessage = "GitHub catalog not checked";
    private JSONArray remotePlugins = new JSONArray();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        readTheme();
        loadCachedCatalog();
        buildUi();
        // Checks on launch only; no background service, no automatic installation.
        refreshGitHubCatalog();
    }

    @Override protected void onDestroy() {
        network.shutdownNow();
        super.onDestroy();
    }

    private void readTheme() {
        SharedPreferences prefs = getSharedPreferences("boxtune.preferences", MODE_PRIVATE);
        darkMode = prefs.getBoolean("dark_mode", true);
    }

    private void saveTheme() {
        getSharedPreferences("boxtune.preferences", MODE_PRIVATE)
                .edit().putBoolean("dark_mode", darkMode).apply();
    }

    private TextView addLabel(LinearLayout parent, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(darkMode ? 0xffe2e8f0 : 0xff172033);
        label.setTextSize(16f);
        label.setPadding(0, 11, 0, 11);
        parent.addView(label);
        return label;
    }

    private void addButton(LinearLayout parent, String text, int id) {
        Button button = new Button(this);
        button.setText(text);
        button.setId(id);
        button.setTextSize(desktopMode ? 14f : 18f);
        button.setMinimumHeight(desktopMode ? 46 : 76);
        button.setOnClickListener(this);
        parent.addView(button);
    }

    private void addCandidate(LinearLayout parent, String pkg) {
        Button button = new Button(this);
        button.setText("REVIEW / UNINSTALL: " + pkg);
        button.setTag(pkg); // distinguishes candidates from numbered main buttons
        button.setOnClickListener(this);
        button.setMinimumHeight(62);
        parent.addView(button);
    }

    private void buildUi() {
        ScrollView scroller = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(darkMode ? 0xff0d1523 : 0xfff1f5f9);
        TextView title = addLabel(layout, "BOXTUNE LITE 2.3");
        title.setTextSize(26f);
        addLabel(layout, "Android 8+ | 2 GB TV box | safe, permission-first tools");
        status = addLabel(layout, "Loading hardware diagnostics...");
        addButton(layout, "REFRESH HARDWARE + INPUT + NETWORK", 1);
        addButton(layout, "TOGGLE DARK / LIGHT MODE", 20);

        addLabel(layout, "KEYBOARD + MOUSE");
        addButton(layout, desktopMode ? "SWITCH TO TV REMOTE LAYOUT" : "SWITCH TO COMPACT MOUSE LAYOUT", 2);
        addButton(layout, "KEYBOARD / INPUT SETTINGS", 3);
        addButton(layout, "BLUETOOTH PAIRING", 4);
        addButton(layout, "ACCESSIBILITY OPTIONS", 5);
        addLabel(layout, "Connect USB keyboard/mouse (or 2.4GHz USB receiver) before testing. " +
                "Compact mode changes this app button size, not the whole Android launcher. " +
                "Roblox and Discord may handle mouse/keyboard differently.");

        addLabel(layout, "MOUSE SPEED (ADVANCED ACCESS)");
        addButton(layout, "ALLOW MODIFY SYSTEM SETTINGS", 6);
        addButton(layout, "MOUSE SPEED: NORMAL (RESTORE)", 7);
        addButton(layout, "MOUSE SPEED: FAST (+3)", 8);
        addButton(layout, "MOUSE SPEED: VERY FAST (+6)", 9);
        addLabel(layout, "Only mouse pointer speed is changed. Approve Modify System Settings " +
                "if Android asks. No Device Administrator or root required.");

        addLabel(layout, "UNUSED APP SCANNER (OPTIONAL USAGE ACCESS)");
        addButton(layout, "1. GRANT USAGE ACCESS IN ANDROID", 21);
        addButton(layout, "2. SCAN APPS NOT SEEN IN LAST 14 DAYS", 22);
        addLabel(layout, "Scan is an estimate: Android usage records may be incomplete. " +
                "System apps and BoxTune are excluded. Tap a candidate to request Android " +
                "uninstall confirmation; nothing is removed automatically.");
        if (scanMode) scanCandidates(layout);

        addLabel(layout, "SAFE PERFORMANCE TOOLS");
        addButton(layout, "INTERNAL STORAGE CLEANUP", 10);
        addButton(layout, "MANAGE INSTALLED APPS", 11);
        addButton(layout, "ROBLOX APP STORAGE / CACHE", 12);
        addButton(layout, "DISCORD APP STORAGE / CACHE", 13);
        addButton(layout, "DISPLAY / RESOLUTION", 14);
        addButton(layout, "BATTERY / POWER SETTINGS", 15);
        addButton(layout, "DEVELOPER OPTIONS", 16);
        addButton(layout, "WI-FI / NETWORK SETTINGS", 17);
        addButton(layout, "OPEN ROBLOX", 18);
        addButton(layout, "OPEN DISCORD", 19);
        addLabel(layout, "INPUT LAG: Prefer a wired USB or 2.4 GHz receiver mouse; " +
                "disable TV motion smoothing; test 720p; set Roblox graphics to Manual/Low. " +
                "Pointer speed changes cursor movement, not FPS.");
        addLabel(layout, "NETWORK: Prefer Ethernet, or 5 GHz Wi-Fi if supported; " +
                "keep streaming downloads stopped while gaming. Wi-Fi link speed is not internet ping. " +
                "No fake DNS or network boosting.");
        addButton(layout, "NETWORK / CONNECTION SETTINGS", 23);
        addButton(layout, "DISPLAY / GAME MODE SETTINGS", 24);

        addLabel(layout, "OPTIONAL PLUGINS / COMPANION APPS (PLAY STORE)");
        addButton(layout, "INSTALL AIDA64 HARDWARE DIAGNOSTICS", 25);
        addButton(layout, "INSTALL FILES BY GOOGLE CLEANUP TOOL", 26);
        addLabel(layout, "Plugin buttons open official Play Store pages when available. " +
                "No background downloads, auto-installs, or third-party code is loaded into BoxTune.");
        addLabel(layout, "SYSTEM PREP: Reboot, remove unused apps you recognize, free storage, " +
                "and close streaming apps manually. No root commands, hidden services, or " +
                "automatic task killing.");
        addLabel(layout, "ADVANCED ACCESS: Optional Modify System Settings (pointer speed) and " +
                "Usage Access (read-only usage stats). Neither grants root. Uninstall remains under your control.");

        addLabel(layout, "COMMUNITY PLUGINS — LOCAL JSON IMPORTS");
        addLabel(layout, "Community plugins are data only. Each file requests one limited, reversible action. " +
                 "No downloaded code, shell commands, hidden privileges, or auto-installation.");
        addButton(layout, "IMPORT .JSON COMMUNITY PLUGIN", 27);
        renderPending(layout);
        renderBuiltins(layout);
        renderCommunity(layout);
        addButton(layout, confirmClear ? "CONFIRM: REMOVE ALL COMMUNITY PLUGINS" :
                  "REMOVE ALL COMMUNITY PLUGINS (TWO-STEP)", 28);

        addLabel(layout, "GITHUB COMMUNITY CATALOG — AUTO-REFRESH ON LAUNCH");
        addLabel(layout, "Source: github.com/" + REPOSITORY + "/tree/main/plugins");
        addLabel(layout, catalogMessage);
        addButton(layout, fetching ? "CHECKING GITHUB..." : "REFRESH GITHUB PLUGINS", 41);
        addLabel(layout, "New plugin listings appear after a refresh. Select a plugin to fetch and " +
                "review its JSON before install. Changes are NEVER silently installed.");
        renderGitHubCatalog(layout);

        scroller.addView(layout);
        setContentView(scroller);
        refresh();
    }

    private String inputSummary() {
        int[] ids = InputDevice.getDeviceIds();
        boolean mouse = false;
        boolean keyboard = false;
        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null) {
                mouse |= device.supportsSource(InputDevice.SOURCE_MOUSE);
                keyboard |= device.supportsSource(InputDevice.SOURCE_KEYBOARD);
            }
        }
        return "\nInput devices: " + ids.length + " | Mouse detected: " + mouse
                + " | Keyboard detected: " + keyboard;
    }

    private String networkSummary() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        if (current == null || !current.isConnected()) return "Disconnected / no active network";
        StringBuilder out = new StringBuilder(current.getTypeName()).append(" connected");
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            WifiInfo wifi = wm.getConnectionInfo();
            if (wifi != null && wifi.getLinkSpeed() > 0) {
                out.append(" | Wi-Fi link ").append(wifi.getLinkSpeed()).append(" Mbps (not ping)");
            }
        }
        return out.toString();
    }

    private void refresh() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        ConfigurationInfo cfg = am.getDeviceConfigurationInfo();
        StatFs disk = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        StringBuilder out = new StringBuilder();
        out.append("Model: ").append(Build.MODEL)
           .append("\nHardware: ").append(Build.HARDWARE)
           .append("\nBoard: ").append(Build.BOARD)
           .append("\nAndroid: ").append(Build.VERSION.RELEASE)
           .append("\nGPU OpenGL ES: ").append(cfg.getGlEsVersion())
           .append("\nRAM available: ").append(mem.availMem / 1048576L)
           .append(" / ").append(mem.totalMem / 1048576L).append(" MB")
           .append("\nStorage free: ").append(disk.getAvailableBytes() / 1048576L)
           .append(" / ").append(disk.getTotalBytes() / 1048576L).append(" MB")
           .append(inputSummary())
           .append("\nNetwork: ").append(networkSummary())
           .append("\nModify System Settings: ").append(Settings.System.canWrite(this))
           .append("\n\nRoblox requires a compatible GPU. Diagnostics cannot add RAM or improve FPS by themselves.");
        status.setText(out.toString());
    }

    /**
     * Approximation only. Package presence without recorded foreground activity in the
     * window is a *candidate*, not evidence that the user never needs the app.
     * Only the 15 first user-installed candidates are shown to avoid TV UI overload.
     */
    private void scanCandidates(LinearLayout parent) {
        UsageStatsManager mgr = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        Map<String, UsageStats> observed = mgr.queryAndAggregateUsageStats(now - 1209600000L, now);
        if (observed == null || observed.isEmpty()) {
            addLabel(parent, "Usage records unavailable. Grant Usage Access to BoxTune in Android " +
                    "settings, then scan again.");
            return;
        }
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        Iterator<ApplicationInfo> it = packages.iterator();
        addLabel(parent, "LIKELY UNUSED USER APPS (NO USE OBSERVED IN 14 DAYS):");
        int count = 0;
        while (it.hasNext() && count < 15) {
            ApplicationInfo app = it.next();
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (app.packageName.equals(getPackageName())) continue;
            if (observed.containsKey(app.packageName)) continue;
            addCandidate(parent, app.packageName);
            count++;
        }
        if (count == 0) addLabel(parent, "No candidates found (or TV manufacturer limits visibility).");
    }

    private void askUninstall(String pkg) {
        Intent uninstall = new Intent(Intent.ACTION_DELETE);
        uninstall.setData(Uri.parse("package:" + pkg));
        if (uninstall.resolveActivity(getPackageManager()) != null) startActivity(uninstall);
        else Toast.makeText(this,
                "Android uninstall screen unavailable. Use App Settings to review instead.",
                Toast.LENGTH_LONG).show();
    }

    @Override public void onClick(View view) {
        Object tag = view.getTag();
        if (tag instanceof String) {
            String value = (String) tag;
            if (value.startsWith("run:")) runPlugin(value.substring(4));
            else if (value.startsWith("github:")) fetchPluginForReview(value.substring(7));
            else askUninstall(value); // requires Android confirmation
            return;
        }
        switch (view.getId()) {
            case 1: refresh(); break;
            case 2: desktopMode = !desktopMode; buildUi(); break;
            case 3: openAction("android.settings.INPUT_METHOD_SETTINGS"); break;
            case 4: openAction("android.settings.BLUETOOTH_SETTINGS"); break;
            case 5: openAction("android.settings.ACCESSIBILITY_SETTINGS"); break;
            case 6: askSettingsAccess(); break;
            case 7: setMouseSpeed(0); break;
            case 8: setMouseSpeed(3); break;
            case 9: setMouseSpeed(6); break;
            case 10: openAction("android.settings.INTERNAL_STORAGE_SETTINGS"); break;
            case 11: openAction("android.settings.APPLICATION_SETTINGS"); break;
            case 12: openAppDetails("com.roblox.client"); break;
            case 13: openAppDetails("com.discord"); break;
            case 14: openAction("android.settings.DISPLAY_SETTINGS"); break;
            case 15: openAction("android.settings.BATTERY_SAVER_SETTINGS"); break;
            case 16: openAction("android.settings.APPLICATION_DEVELOPMENT_SETTINGS"); break;
            case 17: openAction("android.settings.WIFI_SETTINGS"); break;
            case 18: launchApp("com.roblox.client"); break;
            case 19: launchApp("com.discord"); break;
            case 20: darkMode = !darkMode; saveTheme(); buildUi(); break;
            case 21: openAction("android.settings.USAGE_ACCESS_SETTINGS"); break;
            case 22: scanMode = true; buildUi(); break;
            case 23: openAction("android.settings.WIRELESS_SETTINGS"); break;
            case 24: openAction("android.settings.DISPLAY_SETTINGS"); break;
            case 25: openStore("https://play.google.com/store/apps/details?id=com.finalwire.aida64"); break;
            case 26: openStore("https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files"); break;
            case 27: choosePlugin(); break;
            case 28:
                if (confirmClear) {
                    getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE).edit()
                            .remove("installed").apply();
                    confirmClear = false;
                    Toast.makeText(this, "Community plugins cleared", Toast.LENGTH_SHORT).show();
                } else confirmClear = true;
                buildUi(); break;
            case 29: approvePending(); break;
            case 30: discardPending(); break;
            case 31: runPlugin("storage"); break;
            case 32: runPlugin("apps"); break;
            case 33: runPlugin("display"); break;
            case 34: runPlugin("network"); break;
            case 35: runPlugin("mouse"); break;
            case 36: runPlugin("dark"); break;
            case 37: runPlugin("compact"); break;
            case 38: runPlugin("diagnostics"); break;
            case 39: runPlugin("scanner"); break;
            case 40: runPlugin("roblox"); break;
            case 41: refreshGitHubCatalog(); break;
        }
    }

    // Only an allowlist of actions is interpreted; nothing is dynamically executed.
    private static boolean allowedAction(String category, String action) {
        if ("System".equals(category)) return action.matches("storage|apps|display|network|mouse");
        if ("App".equals(category)) return action.matches("dark|compact|diagnostics|scanner|roblox");
        return false;
    }

    private void runPlugin(String action) {
        switch (action) {
            case "storage": openAction("android.settings.INTERNAL_STORAGE_SETTINGS"); break;
            case "apps": openAction("android.settings.APPLICATION_SETTINGS"); break;
            case "display": openAction("android.settings.DISPLAY_SETTINGS"); break;
            case "network": openAction("android.settings.WIRELESS_SETTINGS"); break;
            case "mouse": askSettingsAccess(); break;
            case "dark": darkMode = !darkMode; saveTheme(); buildUi(); break;
            case "compact": desktopMode = !desktopMode; buildUi(); break;
            case "diagnostics": refresh(); break;
            case "scanner": scanMode = true; buildUi(); break;
            case "roblox": launchApp("com.roblox.client"); break;
            default: Toast.makeText(this, "Unsupported plugin action", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderBuiltins(LinearLayout layout) {
        addLabel(layout, "SYSTEM PLUGINS (5 BUILT-IN)");
        addButton(layout, "Storage Sweep — storage review", 31);
        addButton(layout, "Startup Audit — review installed apps", 32);
        addButton(layout, "Display Tuner — display settings", 33);
        addButton(layout, "Connection Check — network settings", 34);
        addButton(layout, "Pointer Tuner — advanced pointer controls", 35);
        addLabel(layout, "APP PLUGINS (5 BUILT-IN)");
        addButton(layout, "Midnight Skin — toggle theme", 36);
        addButton(layout, "Compact Workspace — compact UI", 37);
        addButton(layout, "Health Dashboard — live diagnostics", 38);
        addButton(layout, "App Review — unused apps scanner", 39);
        addButton(layout, "Roblox Hub — launch Roblox", 40);
    }

    private JSONArray installedPlugins() {
        try {
            return new JSONArray(getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE)
                    .getString("installed", "[]"));
        } catch (Exception ex) { return new JSONArray(); }
    }

    private void renderCommunity(LinearLayout parent) {
        addLabel(parent, "IMPORTED COMMUNITY PLUGINS");
        JSONArray installed = installedPlugins();
        if (installed.length() == 0) addLabel(parent, "No community plugins installed yet.");
        for (int i = 0; i < installed.length() && i < 20; i++) {
            JSONObject plugin = installed.optJSONObject(i);
            if (plugin == null) continue;
            String category = plugin.optString("category", "");
            String action = plugin.optString("action", "");
            if (!allowedAction(category, action)) continue;
            Button button = new Button(this);
            button.setText(category + " | " + plugin.optString("name", "Community Plugin")
                    + " — " + plugin.optString("description", "") + " [RUN]");
            button.setTag("run:" + action);
            button.setOnClickListener(this);
            parent.addView(button);
        }
    }

    private void renderPending(LinearLayout layout) {
        String text = getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE).getString("pending", "");
        if (text.length() == 0) return;
        try {
            JSONObject p = new JSONObject(text);
            addLabel(layout, "REVIEW PENDING PLUGIN: " + p.optString("name") +
                    " | " + p.optString("category") + " | Action: " + p.optString("action") +
                    " | " + p.optString("description"));
            addButton(layout, "APPROVE AND INSTALL THIS PLUGIN", 29);
            addButton(layout, "REJECT PENDING PLUGIN", 30);
        } catch (Exception ex) { discardPending(); }
    }

    private void choosePlugin() {
        Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
        picker.setType("*/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        if (picker.resolveActivity(getPackageManager()) != null)
            startActivityForResult(picker, PICK_PLUGIN);
        else Toast.makeText(this, "No document picker installed on this TV box", Toast.LENGTH_LONG).show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_PLUGIN || result != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = getContentResolver().openInputStream(data.getData());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalArgumentException("Could not open plugin file");
            byte[] buffer = new byte[2048];
            int n; int total = 0;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > 12288) throw new IllegalArgumentException("Plugin file exceeds 12 KiB");
                out.write(buffer, 0, n);
            }
            JSONObject p = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            String category = p.optString("category", "");
            String action = p.optString("action", "");
            String id = p.optString("id", "");
            String name = p.optString("name", "");
            if (p.optInt("schema", 0) != 1 || !id.matches("[a-z0-9._-]{1,40}") ||
                    name.isEmpty() || name.length() > 64 ||
                    p.optString("description", "").length() > 160 ||
                    !allowedAction(category, action)) {
                throw new IllegalArgumentException("Unsupported or unsafe plugin manifest");
            }
            JSONObject safe = new JSONObject();
            safe.put("schema", 1); safe.put("id", id); safe.put("name", name);
            safe.put("category", category); safe.put("action", action);
            safe.put("description", p.optString("description", ""));
            safe.put("version", p.optInt("version", 1));
            getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE).edit()
                    .putString("pending", safe.toString()).apply();
            buildUi();
        } catch (Exception ex) {
            Toast.makeText(this, "Plugin import rejected: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void approvePending() {
        SharedPreferences prefs = getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE);
        try {
            JSONObject next = new JSONObject(prefs.getString("pending", ""));
            if (!allowedAction(next.optString("category", ""), next.optString("action", "")))
                throw new IllegalArgumentException("Unsupported plugin");
            JSONArray installed = installedPlugins();
            for (int i = 0; i < installed.length(); i++) {
                JSONObject existing = installed.optJSONObject(i);
                if (existing != null && existing.optString("id").equals(next.optString("id"))) {
                    if (!existing.optString("category").equals(next.optString("category")) ||
                        !existing.optString("action").equals(next.optString("action")))
                        throw new IllegalArgumentException("An update cannot change plugin permissions");
                    installed.remove(i);  // same allowed action; explicit reapproval above
                    break;
                }
            }
            if (installed.length() >= 20) throw new IllegalArgumentException("Limit: 20 community plugins");
            installed.put(next);
            prefs.edit().putString("installed", installed.toString()).remove("pending").apply();
            Toast.makeText(this, "Community plugin installed", Toast.LENGTH_SHORT).show();
            buildUi();
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void discardPending() {
        getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE).edit().remove("pending").apply();
        buildUi();
    }

    // GitHub integration: bounded HTTPS reads, fixed raw.githubusercontent.com origin,
    // strict manifest allowlist, and explicit per-plugin approval. No remote code execution.
    private static String readHttps(String url, int maxBytes) throws Exception {
        URL parsed = new URL(url);
        if (!"https".equals(parsed.getProtocol()) ||
                !"raw.githubusercontent.com".equals(parsed.getHost()))
            throw new IllegalArgumentException("Only the configured GitHub host is supported");
        HttpURLConnection connection = (HttpURLConnection) parsed.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IllegalArgumentException("GitHub HTTP " + status +
                    "; repository might not be published yet");
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int read, count = 0;
                while ((read = in.read(buffer)) != -1) {
                    count += read;
                    if (count > maxBytes) throw new IllegalArgumentException("Download too large");
                    result.write(buffer, 0, read);
                }
                return new String(result.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
    }

    private static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9._-]{1,40}");
    }

    private static JSONObject validatePlugin(JSONObject p) throws Exception {
        String category = p.optString("category", "");
        String action = p.optString("action", "");
        String id = p.optString("id", "");
        String name = p.optString("name", "");
        int version = p.optInt("version", 1);
        if (p.optInt("schema", 0) != 1 || !validId(id) ||
                name.length() == 0 || name.length() > 64 ||
                p.optString("description", "").length() > 160 ||
                version < 1 || version > 100000 || !allowedAction(category, action))
            throw new IllegalArgumentException("Invalid or unsafe plugin manifest");
        JSONObject safe = new JSONObject();
        safe.put("schema", 1);
        safe.put("id", id); safe.put("name", name);
        safe.put("description", p.optString("description", ""));
        safe.put("category", category); safe.put("action", action);
        safe.put("version", version);
        return safe;
    }

    private static JSONArray parseCatalog(String json) throws Exception {
        JSONObject response = new JSONObject(json);
        if (response.optInt("schema", 0) != 1) throw new IllegalArgumentException("Unsupported catalog schema");
        JSONArray entries = response.optJSONArray("plugins");
        if (entries == null || entries.length() > 100)
            throw new IllegalArgumentException("Invalid catalog size (max 100)");
        JSONArray clean = new JSONArray();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject raw = entries.optJSONObject(i);
            if (raw == null) throw new IllegalArgumentException("Invalid catalog item");
            JSONObject item = validatePlugin(raw);
            String path = raw.optString("path", "");
            String prefix = "System".equals(item.getString("category")) ? "system/" : "app/";
            if (!path.startsWith(prefix) || !path.matches("(?:system|app)/[a-z0-9._-]+\\.json"))
                throw new IllegalArgumentException("Invalid plugin path");
            if (!ids.add(item.getString("id"))) throw new IllegalArgumentException("Duplicate plugin ID");
            item.put("path", path);
            clean.put(item);
        }
        return clean;
    }

    private void loadCachedCatalog() {
        try {
            SharedPreferences prefs = getSharedPreferences("boxtune.github.catalog", MODE_PRIVATE);
            String old = prefs.getString("catalog", "");
            if (old.length() > 0) {
                remotePlugins = parseCatalog(old);
                catalogMessage = "Cached catalog: " + remotePlugins.length() + " plugins (may be outdated)";
            }
        } catch (Exception ignored) {
            remotePlugins = new JSONArray();
            catalogMessage = "Cached catalog invalid; refresh required";
        }
    }

    private void refreshGitHubCatalog() {
        if (fetching) return;
        fetching = true;
        catalogMessage = "Checking GitHub...";
        buildUi();
        network.execute(() -> {
            try {
                String data = readHttps(CATALOG_URL, CATALOG_LIMIT);
                JSONArray parsed = parseCatalog(data);
                ui.post(() -> {
                    fetching = false;
                    remotePlugins = parsed;
                    // Store only data that passed validation.
                    getSharedPreferences("boxtune.github.catalog", MODE_PRIVATE)
                        .edit().putString("catalog", data).apply();
                    catalogMessage = "Up to date: " + parsed.length() + " plugins on GitHub";
                    buildUi();
                });
            } catch (Exception ex) {
                String error = ex.getMessage();
                ui.post(() -> {
                    fetching = false;
                    catalogMessage = "GitHub check failed: " + error +
                            ". Saved catalog remains available.";
                    buildUi();
                });
            }
        });
    }

    private void renderGitHubCatalog(LinearLayout layout) {
        JSONArray installed = installedPlugins();
        for (int i = 0; i < remotePlugins.length(); i++) {
            JSONObject item = remotePlugins.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "");
            int availableVersion = item.optInt("version", 1);
            String installedLabel = "NEW";
            for (int j = 0; j < installed.length(); j++) {
                JSONObject local = installed.optJSONObject(j);
                if (local != null && id.equals(local.optString("id"))) {
                    installedLabel = local.optInt("version", 1) < availableVersion ? "UPDATE" : "INSTALLED";
                    break;
                }
            }
            Button button = new Button(this);
            button.setText(installedLabel + " | " + item.optString("category") + " | " +
                    item.optString("name") + " v" + availableVersion + " — REVIEW");
            button.setTag("github:" + item.optString("path"));
            button.setOnClickListener(this);
            layout.addView(button);
        }
    }

    private void fetchPluginForReview(String path) {
        if (fetching) return;
        JSONObject listing = null;
        for (int i = 0; i < remotePlugins.length(); i++) {
            JSONObject p = remotePlugins.optJSONObject(i);
            if (p != null && path.equals(p.optString("path"))) { listing = p; break; }
        }
        if (listing == null) {
            Toast.makeText(this, "Plugin not in validated GitHub catalog", Toast.LENGTH_LONG).show();
            return;
        }
        final JSONObject expected = listing;
        fetching = true;
        catalogMessage = "Downloading " + expected.optString("name") + "...";
        buildUi();
        network.execute(() -> {
            try {
                String raw = readHttps(RAW_BASE + path, PLUGIN_LIMIT);
                JSONObject downloaded = validatePlugin(new JSONObject(raw));
                if (!downloaded.getString("id").equals(expected.getString("id")) ||
                        !downloaded.getString("category").equals(expected.getString("category")) ||
                        !downloaded.getString("action").equals(expected.getString("action")) ||
                        downloaded.getInt("version") != expected.getInt("version"))
                    throw new IllegalArgumentException("Plugin differs from the GitHub catalog");
                ui.post(() -> {
                    fetching = false;
                    getSharedPreferences(PLUGIN_PREFS, MODE_PRIVATE).edit()
                            .putString("pending", downloaded.toString()).apply();
                    catalogMessage = "Downloaded plugin. Review and approve to install.";
                    buildUi();
                });
            } catch (Exception ex) {
                String error = ex.getMessage();
                ui.post(() -> {
                    fetching = false;
                    catalogMessage = "Plugin download failed: " + error;
                    buildUi();
                });
            }
        });
    }

    private void askSettingsAccess() {
        Intent intent = new Intent("android.settings.action.MANAGE_WRITE_SETTINGS");
        intent.setData(Uri.parse("package:com.boxtune.lite"));
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Toast.makeText(this, "Your TV firmware has no settings-access page.", Toast.LENGTH_LONG).show();
    }

    private void setMouseSpeed(int value) {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Grant Modify System Settings, then try again.", Toast.LENGTH_LONG).show();
            askSettingsAccess();
            return;
        }
        boolean applied = Settings.System.putInt(getContentResolver(), "pointer_speed", value);
        Toast.makeText(this, applied ? "Mouse speed saved. Reconnect mouse if needed."
                : "TV firmware rejected mouse speed change.", Toast.LENGTH_LONG).show();
        refresh();
    }

    private void openAction(String action) {
        Intent intent = new Intent(action);
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Toast.makeText(this, "This settings page is unavailable on your TV box.", Toast.LENGTH_LONG).show();
    }

    private void openAppDetails(String pkg) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.parse("package:" + pkg));
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Toast.makeText(this, "App details unavailable on this firmware.", Toast.LENGTH_LONG).show();
    }

    private void launchApp(String pkg) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) startActivity(intent);
        else Toast.makeText(this, "App not installed or missing TV launcher.", Toast.LENGTH_LONG).show();
    }

    private void openStore(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Toast.makeText(this, "No browser/Play Store available. Companion tool was not installed.",
                Toast.LENGTH_LONG).show();
    }
}
