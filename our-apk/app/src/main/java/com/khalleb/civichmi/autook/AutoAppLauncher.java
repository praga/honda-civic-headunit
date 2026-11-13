package com.khalleb.civichmi.autook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

public class AutoAppLauncher {

    public static final String PREF_AUTO_START_APP = "auto_start_app";
    public static final String PREF_AUTO_START_DELAY = "auto_start_app_delay";
    public static final String PREF_AUTO_START_SEND_PLAY = "auto_start_app_send_play_command";

    private final Context context;
    private volatile boolean launched;

    public AutoAppLauncher(Context context) {
        this.context = context;
    }

    public synchronized void scheduleLaunch() {
        if (launched) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String configuredPkg = prefs.getString(PREF_AUTO_START_APP, AutoOkConfig.AUTO_START_APP_PKG);
        boolean enabled = AutoOkConfig.ENABLE_AUTO_START_APP || (configuredPkg != null && !configuredPkg.trim().isEmpty());

        if (!shouldLaunch(enabled, configuredPkg)) {
            return;
        }

        launched = true;
        final String targetPkg = configuredPkg.trim();
        final long delayMs = prefs.getInt(PREF_AUTO_START_DELAY, (int) (AutoOkConfig.AUTO_START_APP_DELAY_MS / 1000L)) * 1000L;
        final boolean sendPlayKey = prefs.getBoolean(PREF_AUTO_START_SEND_PLAY, AutoOkConfig.AUTO_START_SEND_PLAY_KEY);

        Thread launcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                    launchApp(targetPkg);
                    if (sendPlayKey) {
                        Thread.sleep(AutoOkConfig.AUTO_START_PLAY_KEY_DELAY_MS);
                        sendPlayKeyEvent();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    FileLogger.log("AutoAppLauncher: launch failed: " + t.getMessage());
                }
            }
        }, "AutoOk-AutoAppLauncher");
        launcherThread.setDaemon(true);
        launcherThread.start();
    }

    private void launchApp(String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                FileLogger.log("AutoAppLauncher: launched " + pkg);
            } else {
                FileLogger.log("AutoAppLauncher: launch intent not found for " + pkg);
            }
        } catch (Throwable t) {
            FileLogger.log("AutoAppLauncher: error launching " + pkg + ": " + t.getMessage());
        }
    }

    private void sendPlayKeyEvent() {
        try {
            boolean ok = Shell.su("input keyevent 126");
            FileLogger.log("AutoAppLauncher: sent KEYCODE_MEDIA_PLAY (126) ok=" + ok);
        } catch (Throwable t) {
            FileLogger.log("AutoAppLauncher: error sending play key: " + t.getMessage());
        }
    }

    public static boolean shouldLaunch(boolean enabled, String pkg) {
        return enabled && pkg != null && !pkg.trim().isEmpty();
    }
}
