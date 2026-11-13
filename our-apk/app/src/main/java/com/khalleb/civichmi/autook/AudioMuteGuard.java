package com.khalleb.civichmi.autook;

import android.content.Context;
import android.media.AudioManager;

public class AudioMuteGuard {

    private final Context context;
    private final AudioManager audioManager;
    private final CameraVolumeController cameraController;
    private Thread workerThread;
    private volatile boolean running;
    private int stuckCount;

    public AudioMuteGuard(Context context, CameraVolumeController cameraController) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.cameraController = cameraController;
    }

    public synchronized void start() {
        if (!AutoOkConfig.ENABLE_AUDIO_MUTE_GUARD) {
            return;
        }
        if (workerThread != null) {
            return;
        }
        running = true;
        stuckCount = 0;
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                guardLoop();
            }
        }, "AutoOk-AudioMuteGuard");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    private void guardLoop() {
        while (running) {
            try {
                Thread.sleep(AutoOkConfig.AUDIO_MUTE_GUARD_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running) {
                return;
            }
            checkAndFixVolume();
        }
    }

    public void checkAndFixVolume() {
        if (audioManager == null) {
            return;
        }
        boolean camDucked = cameraController != null && cameraController.isDucked();
        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (current == 0) {
                stuckCount++;
            } else {
                stuckCount = 0;
            }

            if (shouldRecover(camDucked, current, stuckCount, AutoOkConfig.AUDIO_MUTE_GUARD_STUCK_THRESHOLD)) {
                int target = AutoOkConfig.AUDIO_MUTE_GUARD_MIN_VOLUME;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
                FileLogger.log("AudioMuteGuard: recovered stuck muted volume from 0 -> " + target);
                stuckCount = 0;
            }
        } catch (Throwable t) {
            FileLogger.log("AudioMuteGuard: check error: " + t.getMessage());
        }
    }

    public static boolean shouldRecover(boolean isCameraDucked, int currentVolume, int stuckCount, int threshold) {
        if (isCameraDucked) {
            return false;
        }
        return currentVolume == 0 && stuckCount >= threshold;
    }
}
