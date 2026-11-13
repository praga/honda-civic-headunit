package com.khalleb.civichmi.autook;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AutoOkService extends Service {

    private final Object watcherLock = new Object();
    private LogcatWatcher watcher;
    private volatile boolean handled;
    private volatile long lastTapAtMs;

    private Thread watchdogThread;
    private volatile boolean watchdogRunning;

    private CameraVolumeController cameraController;
    private AudioMuteGuard muteGuard;
    private AutoAppLauncher appLauncher;
    private GpsTimeSync timeSync;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FileLogger.init(this);
        FileLogger.startSessionOnce();
        if (!isConfigValid()) {
            FileLogger.log("ERRO: config invalida em AutoOkConfig");
            stopSelf();
            return START_NOT_STICKY;
        }

        initSubsystems();

        String action = intent == null ? null : intent.getAction();
        if (AutoOkConfig.ACTION_FORCE_RESTART.equals(action)) {
            String source = intent.getStringExtra("source");
            String cause = intent.getStringExtra("cause");
            FileLogger.log("FORCE_RESTART source=" + source + " cause=" + cause);
            forceRestartWatcher();
            return START_STICKY;
        }

        if (AutoOkConfig.ACTION_DECREASE_VOLUME.equals(action)) {
            if (cameraController != null) {
                cameraController.onCameraEnter();
            }
            return START_STICKY;
        }

        if (AutoOkConfig.ACTION_RESTORE_VOLUME.equals(action) ||
            AutoOkConfig.ACTION_EXIT_REAR_WIDE_CAMERA.equals(action) ||
            AutoOkConfig.ACTION_RESUME_CAMERA.equals(action)) {
            if (cameraController != null) {
                cameraController.onCameraExit();
            }
            return START_STICKY;
        }

        synchronized (watcherLock) {
            if (watcher == null) {
                watcher = buildWatcher();
                watcher.start();
                FileLogger.log("watcher iniciado needle=" + AutoOkConfig.AVISO_ACTIVITY_NAME);
            }
        }
        startWatchdog();
        return START_STICKY;
    }

    private void initSubsystems() {
        if (cameraController == null) {
            cameraController = new CameraVolumeController(this);
        }
        if (muteGuard == null) {
            muteGuard = new AudioMuteGuard(this, cameraController);
            muteGuard.start();
        }
        if (appLauncher == null) {
            appLauncher = new AutoAppLauncher(this);
        }
        if (timeSync == null) {
            timeSync = new GpsTimeSync(this);
            timeSync.start();
        }
    }

    @Override
    public void onDestroy() {
        stopWatchdog();
        stopWatcher();
        if (muteGuard != null) {
            muteGuard.stop();
            muteGuard = null;
        }
        if (timeSync != null) {
            timeSync.stop();
            timeSync = null;
        }
        if (cameraController != null) {
            cameraController.destroy();
            cameraController = null;
        }
        FileLogger.log("service destroyed");
        super.onDestroy();
    }

    private boolean isConfigValid() {
        boolean hasActivity = AutoOkConfig.AVISO_ACTIVITY_NAME != null
            && AutoOkConfig.AVISO_ACTIVITY_NAME.length() > 0;
        boolean hasCoords = AutoOkConfig.BUTTON_OK_X >= 0 && AutoOkConfig.BUTTON_OK_Y >= 0;
        boolean hasFallback = AutoOkConfig.TAP_FALLBACK_COORDS != null
            && AutoOkConfig.TAP_FALLBACK_COORDS.length > 0;
        return hasActivity && hasCoords && hasFallback;
    }

    private LogcatWatcher buildWatcher() {
        return new LogcatWatcher(
            AutoOkConfig.AVISO_ACTIVITY_NAME,
            AutoOkConfig.BOOT_INITIAL_WAIT_MS,
            new AvisoCallback(this),
            new CameraLineListener(this)
        );
    }

    private static final class AvisoCallback implements LogcatWatcher.OnDetected {
        private final AutoOkService service;

        AvisoCallback(AutoOkService service) {
            this.service = service;
        }

        @Override
        public void onDetected() {
            service.onAvisoDetected();
        }
    }

    private static final class CameraLineListener implements LogcatWatcher.LineListener {
        private final AutoOkService service;

        CameraLineListener(AutoOkService service) {
            this.service = service;
        }

        @Override
        public void onLine(String line) {
            service.onLogcatLine(line);
        }
    }

    private void onLogcatLine(String line) {
        if (cameraController == null) {
            return;
        }
        if (CameraActivityMatcher.isCameraActivity(line)) {
            cameraController.onCameraEnter();
        } else if (CameraActivityMatcher.isNonCameraActivity(line)) {
            if (cameraController.isDucked()) {
                cameraController.onCameraExit();
            }
        }
    }

    private void onAvisoDetected() {
        if (handled) {
            return;
        }
        long now = System.currentTimeMillis();
        if (TapDeduper.shouldSuppress(now, lastTapAtMs, AutoOkConfig.TAP_DEDUP_SECONDS)) {
            FileLogger.log("dedup: tap suprimido (" + (now - lastTapAtMs) + "ms desde ultimo)");
            return;
        }
        handled = true;
        FileLogger.log("aviso detectado (NORMAL_STARTUP match)");
        if (!sleepSeconds(AutoOkConfig.DELAY_SECONDS_AFTER_DETECT)) {
            return;
        }
        boolean ok = Shell.suTapWithFallback(
            AutoOkConfig.TAP_FALLBACK_COORDS,
            AutoOkConfig.TAP_FALLBACK_INTERVAL_MS
        );
        lastTapAtMs = System.currentTimeMillis();
        FileLogger.log("tap ok=" + ok + " coords=" + AutoOkConfig.TAP_FALLBACK_COORDS.length);

        if (appLauncher != null) {
            appLauncher.scheduleLaunch();
        }

        rearmWatcher();
    }

    private void rearmWatcher() {
        if (!sleepSeconds(AutoOkConfig.REARM_DELAY_SECONDS)) {
            return;
        }
        handled = false;
        FileLogger.log("rearmed (watcher reusado)");
    }

    private boolean sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void stopWatcher() {
        LogcatWatcher w;
        synchronized (watcherLock) {
            w = watcher;
            watcher = null;
        }
        if (w != null) {
            w.stop();
        }
    }

    private void forceRestartWatcher() {
        synchronized (watcherLock) {
            handled = false;
            LogcatWatcher w = watcher;
            if (w == null || !w.isAlive()) {
                if (w != null) {
                    w.stop();
                }
                watcher = buildWatcher();
                watcher.start();
                FileLogger.log("force restart: watcher recriado (estava morto)");
            } else {
                FileLogger.log("force restart: handled resetado (watcher mantido vivo)");
            }
        }
    }

    private void startWatchdog() {
        if (watchdogThread != null) {
            return;
        }
        watchdogRunning = true;
        watchdogThread = new Thread(new WatchdogRunnable(this), "AutoOk-Watchdog");
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }

    private void stopWatchdog() {
        watchdogRunning = false;
        Thread t = watchdogThread;
        if (t != null) {
            t.interrupt();
        }
        watchdogThread = null;
    }

    private static final class WatchdogRunnable implements Runnable {
        private final AutoOkService service;

        WatchdogRunnable(AutoOkService service) {
            this.service = service;
        }

        @Override
        public void run() {
            service.watchdogLoop();
        }
    }

    private void watchdogLoop() {
        while (watchdogRunning) {
            try {
                Thread.sleep(AutoOkConfig.WATCHDOG_INTERVAL_SECONDS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!watchdogRunning) {
                return;
            }
            ensureWatcherAlive();
        }
    }

    private void ensureWatcherAlive() {
        synchronized (watcherLock) {
            if (handled) {
                return;
            }
            LogcatWatcher w = watcher;
            if (w != null && w.isAlive()) {
                return;
            }
            if (w != null) {
                w.stop();
            }
            watcher = buildWatcher();
            watcher.start();
            FileLogger.log("watchdog: watcher recriado (anterior=" + (w == null ? "null" : "morto") + ")");
        }
    }
}
