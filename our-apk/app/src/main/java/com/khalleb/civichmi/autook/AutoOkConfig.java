package com.khalleb.civichmi.autook;

public final class AutoOkConfig {

    public static final String AVISO_ACTIVITY_NAME = "com.mitsubishielectric.ada.app.dalauncher/.OpeningDivideActivity";

    public static final int BUTTON_OK_X = 400;
    public static final int BUTTON_OK_Y = 350;

    public static final int[][] TAP_FALLBACK_COORDS = new int[][] {
        {400, 350},
        {400, 340},
        {400, 360}
    };

    public static final int TAP_FALLBACK_INTERVAL_MS = 200;

    public static final int DELAY_SECONDS_AFTER_DETECT = 1;

    public static final int REARM_DELAY_SECONDS = 2;

    public static final int BOOT_INITIAL_WAIT_MS = 500;

    public static final int WATCHDOG_INTERVAL_SECONDS = 30;

    public static final int TAP_DEDUP_SECONDS = 3;

    public static final String LOGCAT_TAG = "AutoOk";

    public static final String FILE_LOG_NAME = "autook.log";

    public static final String HMI_SERVICE_START_ACTION =
        "com.mitsubishielectric.ada.appservice.powermanager.ACTION_HMI_SERVICE_START";

    public static final String ACTION_FORCE_RESTART =
        "com.khalleb.civichmi.autook.FORCE_RESTART";

    // Reverse Camera Volume Ducking
    public static final boolean ENABLE_REVERSE_CAMERA_DUCK = true;
    public static final int REVERSE_CAMERA_DUCK_STEPS = 6;
    public static final long REVERSE_CAMERA_DUCK_TIMEOUT_MS = 30000L;
    public static final String CAMERA_PACKAGE_PREFIX = "com.mitsubishielectric.ada.app.camera";
    public static final String ACTION_EXIT_REAR_WIDE_CAMERA =
        "com.mitsubishielectric.android.action.camera.EXIT_REAR_WIDE_CAMERA";
    public static final String ACTION_RESUME_CAMERA =
        "com.mitsubishielectric.android.action.camera.ACTION_RESUME_CAMERA";
    public static final String ACTION_DECREASE_VOLUME =
        "com.khalleb.civichmi.autook.DECREASE_VOLUME";
    public static final String ACTION_RESTORE_VOLUME =
        "com.khalleb.civichmi.autook.RESTORE_VOLUME";

    // Audio Ducking / Mute Glitch Auto-Fix (AudioMuteGuard)
    public static final boolean ENABLE_AUDIO_MUTE_GUARD = true;
    public static final long AUDIO_MUTE_GUARD_INTERVAL_MS = 3000L;
    public static final int AUDIO_MUTE_GUARD_STUCK_THRESHOLD = 2;
    public static final int AUDIO_MUTE_GUARD_MIN_VOLUME = 5;

    // Auto-Start Preferred App on Boot (AutoAppLauncher)
    public static final boolean ENABLE_AUTO_START_APP = false;
    public static final String AUTO_START_APP_PKG = "";
    public static final long AUTO_START_APP_DELAY_MS = 3000L;
    public static final boolean AUTO_START_SEND_PLAY_KEY = true;
    public static final long AUTO_START_PLAY_KEY_DELAY_MS = 3000L;

    // GPS Time Auto-Sync (GpsTimeSync)
    public static final boolean ENABLE_GPS_TIME_SYNC = true;
    public static final long GPS_TIME_SYNC_MAX_DRIFT_SEC = 5L;
    public static final long GPS_TIME_SYNC_MIN_INTERVAL_MS = 60000L;

    // Wireless ADB (WirelessAdbController)
    public static final boolean ENABLE_WIRELESS_ADB = true;
    public static final int WIRELESS_ADB_PORT = 5555;

    private AutoOkConfig() {
    }
}
