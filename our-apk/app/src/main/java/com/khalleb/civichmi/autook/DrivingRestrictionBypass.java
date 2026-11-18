package com.khalleb.civichmi.autook;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Driving Restriction Bypass (Video and App Lockout Override) on the
 * Mitsubishi Andromeda Tegra 3 platform (Honda Civic 10th-Gen).
 *
 * <p>Mechanism:
 * The head unit enforces motion and parking brake lockouts via {@code AppManagerService}
 * ({@code DrivingDistractionBehavior}), which monitors {@code VehicleInfoManagerApService}.
 * When speed > 0 or the parking brake is disengaged, it restricts touchscreen interaction
 * and blanks the view for applications based on their {@code <regulation>} tag in
 * {@code /data/data/whitelist-1.0.xml}:
 * <ul>
 *   <li>{@code 0 / null}: Zero restriction in motion (full touch and video display allowed).</li>
 *   <li>{@code 1 / parktrigger}: Restricted when parking brake is released.</li>
 *   <li>{@code 2 / speedtrigger}: Restricted when vehicle speed > 0.</li>
 *   <li>{@code 3 / park&speedtrigger}: Restricted if moving OR parking brake released.</li>
 * </ul>
 *
 * This class ensures that all existing entries and newly installed apps have
 * {@code <regulation>null</regulation>}, completely bypassing the driving restriction
 * natively without unstable Xposed runtime modifications.
 */
public final class DrivingRestrictionBypass {

    private static final String TAG = AutoOkConfig.LOGCAT_TAG;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static final String REGULATION_NULL = "null";

    // Known media, navigation, and stock packages to ensure are whitelisted
    public static final List<String> DEFAULT_WHITELIST_PACKAGES = Collections.unmodifiableList(Arrays.asList(
        "com.google.android.apps.maps",
        "com.waze",
        "com.spotify.music",
        "org.videolan.vlc",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro",
        "com.zjinnova.zlink",
        "com.autonavi.amapauto",
        "com.android.chrome",
        "com.android.browser",
        "com.android.deskclock",
        "com.android.calendar",
        "com.android.gallery3d",
        "com.android.settings",
        "com.mitsubishielectric.ada.app.avplayer"
    ));

    private static final Pattern REGULATION_TAG_PATTERN = Pattern.compile(
        "<regulation>([^<]*)</regulation>|<regulation\\s*/>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PACKAGE_TAG_PATTERN = Pattern.compile(
        "<package>([^<]+)</package>",
        Pattern.CASE_INSENSITIVE
    );

    private DrivingRestrictionBypass() {
    }

    /**
     * Extracts all package names defined inside {@code <package>...</package>} tags.
     */
    public static Set<String> extractPackageNames(String xml) {
        Set<String> set = new HashSet<String>();
        if (xml == null || xml.isEmpty()) {
            return set;
        }
        Matcher m = PACKAGE_TAG_PATTERN.matcher(xml);
        while (m.find()) {
            set.add(m.group(1).trim());
        }
        return set;
    }

    /**
     * Checks if the given whitelist XML requires updating (i.e. contains non-null regulations
     * or is missing any required packages).
     */
    public static boolean isBypassNeeded(String xml, Collection<String> requiredPackages) {
        if (xml == null || xml.trim().isEmpty() || !xml.contains("<whiteList>")) {
            return true;
        }

        Matcher m = REGULATION_TAG_PATTERN.matcher(xml);
        while (m.find()) {
            String content = m.group(1);
            if (content == null || !REGULATION_NULL.equalsIgnoreCase(content.trim())) {
                return true;
            }
        }

        if (requiredPackages != null && !requiredPackages.isEmpty()) {
            Set<String> existing = extractPackageNames(xml);
            for (String pkg : requiredPackages) {
                if (pkg != null && !pkg.trim().isEmpty() && !existing.contains(pkg.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sanitizes the whitelist XML:
     * 1. Replaces all non-null {@code <regulation>} values with {@code null}.
     * 2. Adds any missing packages from {@code extraPackages} with {@code <regulation>null</regulation>}.
     */
    public static String sanitizeWhitelistXml(String rawXml, Collection<String> extraPackages) {
        if (rawXml == null || rawXml.trim().isEmpty() || !rawXml.contains("<whiteList>")) {
            return buildDefaultWhitelist(extraPackages);
        }

        // 1. Replace all regulation tags with <regulation>null</regulation>
        String sanitized = REGULATION_TAG_PATTERN.matcher(rawXml).replaceAll("<regulation>null</regulation>");

        // 2. Identify missing packages to append
        Set<String> existingPackages = extractPackageNames(sanitized);
        StringBuilder newApps = new StringBuilder();

        if (extraPackages != null) {
            for (String pkg : extraPackages) {
                if (pkg == null) {
                    continue;
                }
                String cleanPkg = pkg.trim();
                if (!cleanPkg.isEmpty() && !existingPackages.contains(cleanPkg)) {
                    newApps.append("        <application>\n")
                           .append("            <property>\n")
                           .append("                <name>").append(cleanPkg).append("</name>\n")
                           .append("                <package>").append(cleanPkg).append("</package>\n")
                           .append("                <versionCode>1-999999999</versionCode>\n")
                           .append("                <keyStoreLists>\n")
                           .append("                    <keyStore>null</keyStore>\n")
                           .append("                </keyStoreLists>\n")
                           .append("            </property>\n")
                           .append("            <controlData>\n")
                           .append("                <withAudio>with</withAudio>\n")
                           .append("                <audioStreamType>null</audioStreamType>\n")
                           .append("                <regulation>null</regulation>\n")
                           .append("                <revert>no</revert>\n")
                           .append("            </controlData>\n")
                           .append("        </application>\n");
                    existingPackages.add(cleanPkg);
                }
            }
        }

        if (newApps.length() > 0) {
            int insertIndex = sanitized.lastIndexOf("</applicationLists>");
            if (insertIndex >= 0) {
                sanitized = sanitized.substring(0, insertIndex) + newApps.toString() + sanitized.substring(insertIndex);
            }
        }

        return sanitized;
    }

    /**
     * Synthesizes a clean base whitelist XML when no existing file is found.
     */
    public static String buildDefaultWhitelist(Collection<String> extraPackages) {
        Set<String> allPackages = new HashSet<String>(DEFAULT_WHITELIST_PACKAGES);
        if (extraPackages != null) {
            for (String pkg : extraPackages) {
                if (pkg != null && !pkg.trim().isEmpty()) {
                    allPackages.add(pkg.trim());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<whiteList>\n");
        sb.append("    <formatVersion>1.0</formatVersion>\n");
        sb.append("    <contentVersion>1.5</contentVersion>\n");
        sb.append("    <applicationLists>\n");

        for (String pkg : allPackages) {
            sb.append("        <application>\n");
            sb.append("            <property>\n");
            sb.append("                <name>").append(pkg).append("</name>\n");
            sb.append("                <package>").append(pkg).append("</package>\n");
            sb.append("                <versionCode>1-999999999</versionCode>\n");
            sb.append("                <keyStoreLists>\n");
            sb.append("                    <keyStore>null</keyStore>\n");
            sb.append("                </keyStoreLists>\n");
            sb.append("            </property>\n");
            sb.append("            <controlData>\n");
            sb.append("                <withAudio>with</withAudio>\n");
            sb.append("                <audioStreamType>null</audioStreamType>\n");
            sb.append("                <regulation>null</regulation>\n");
            sb.append("                <revert>no</revert>\n");
            sb.append("            </controlData>\n");
            sb.append("        </application>\n");
        }

        sb.append("    </applicationLists>\n");
        sb.append("</whiteList>\n");
        return sb.toString();
    }

    /**
     * Executes bypass synchronously on the device using root commands.
     */
    public static synchronized boolean applyBypass(Context context) {
        if (!AutoOkConfig.ENABLE_DRIVING_RESTRICTION_BYPASS) {
            return false;
        }

        FileLogger.log("DrivingRestrictionBypass: checking whitelist regulations...");
        String currentXml = Shell.suExecCapture("cat " + AutoOkConfig.WHITELIST_PATH);

        Set<String> packagesToEnsure = new HashSet<String>(DEFAULT_WHITELIST_PACKAGES);

        // Discover user-installed third-party packages
        String pmOut = Shell.suExecCapture("pm list packages -3");
        if (pmOut != null && !pmOut.isEmpty()) {
            for (String line : pmOut.split("\\r?\\n")) {
                line = line.trim();
                if (line.startsWith("package:")) {
                    String pkg = line.substring("package:".length()).trim();
                    if (!pkg.isEmpty()) {
                        packagesToEnsure.add(pkg);
                    }
                }
            }
        }

        if (!isBypassNeeded(currentXml, packagesToEnsure)) {
            FileLogger.log("DrivingRestrictionBypass: all regulations already unlocked (null); no change needed");
            return true;
        }

        String sanitizedXml = sanitizeWhitelistXml(currentXml, packagesToEnsure);
        if (sanitizedXml == null || sanitizedXml.trim().isEmpty()) {
            FileLogger.log("DrivingRestrictionBypass: error sanitizing XML (empty result)");
            return false;
        }

        try {
            File cacheDir = context.getCacheDir();
            File tmpFile = new File(cacheDir, "whitelist-autook-tmp.xml");
            FileOutputStream fos = new FileOutputStream(tmpFile);
            fos.write(sanitizedXml.getBytes("UTF-8"));
            fos.flush();
            fos.close();

            // Backup original once if not already backed up
            Shell.suExec("if [ ! -e /data/data/whitelist-1.0.xml.bak ]; then cp "
                + AutoOkConfig.WHITELIST_PATH + " /data/data/whitelist-1.0.xml.bak; fi");

            // Copy sanitized XML to target location
            boolean cpOk = Shell.suExec("cp " + tmpFile.getAbsolutePath() + " " + AutoOkConfig.WHITELIST_PATH);
            Shell.suExec("chmod 666 " + AutoOkConfig.WHITELIST_PATH);
            Shell.suExec("chown system:system " + AutoOkConfig.WHITELIST_PATH);
            tmpFile.delete();

            if (!cpOk) {
                FileLogger.log("DrivingRestrictionBypass: copy to " + AutoOkConfig.WHITELIST_PATH + " failed");
                return false;
            }

            // Signal system to reload whitelist
            Shell.suExec("am broadcast -a android.intent.action.PACKAGE_REPLACED -d package:com.honda.displayaudio.whitelist");
            Shell.suExec("pkill -f com.mitsubishielectric.ada.appservice.whitelist");
            Shell.suExec("pkill -f com.mitsubishielectric.ada.appservice.appmanagerservice");

            FileLogger.log("DrivingRestrictionBypass: driving restriction bypass applied successfully");
            return true;
        } catch (Exception e) {
            FileLogger.log("DrivingRestrictionBypass exception: " + e.getMessage());
            Log.e(TAG, "DrivingRestrictionBypass exception", e);
            return false;
        }
    }

    /**
     * Executes bypass asynchronously on a background worker thread.
     */
    public static void applyBypassAsync(final Context context) {
        if (!AutoOkConfig.ENABLE_DRIVING_RESTRICTION_BYPASS) {
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    applyBypass(context);
                } catch (Throwable t) {
                    FileLogger.log("DrivingRestrictionBypass async error: " + t.getMessage());
                    Log.e(TAG, "DrivingRestrictionBypass async error", t);
                }
            }
        });
    }

    public static void onPackageInstalled(final Context context, final String packageName) {
        FileLogger.log("DrivingRestrictionBypass: package installed/replaced: " + packageName);
        applyBypassAsync(context);
    }
}
