package com.khalleb.civichmi.autook;

import android.content.Context;
import android.media.AudioManager;

import java.util.Timer;
import java.util.TimerTask;

public class CameraVolumeController {

    private final Context context;
    private final AudioManager audioManager;
    private volatile boolean ducked;
    private volatile int originalVolume = -1;
    private Timer timeoutTimer;

    public CameraVolumeController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public synchronized boolean isDucked() {
        return ducked;
    }

    public synchronized int getOriginalVolume() {
        return originalVolume;
    }

    public synchronized void onCameraEnter() {
        if (!AutoOkConfig.ENABLE_REVERSE_CAMERA_DUCK) {
            return;
        }
        if (audioManager == null) {
            return;
        }
        if (ducked) {
            resetTimeoutTimer();
            return;
        }

        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            originalVolume = current;
            int target = calculateDuckedVolume(current, AutoOkConfig.REVERSE_CAMERA_DUCK_STEPS);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            ducked = true;
            resetTimeoutTimer();
            FileLogger.log("CameraVolumeController: ducked volume " + current + " -> " + target);
        } catch (Throwable t) {
            FileLogger.log("CameraVolumeController: error on duck: " + t.getMessage());
        }
    }

    public synchronized void onCameraExit() {
        if (!ducked) {
            return;
        }
        cancelTimeoutTimer();
        if (audioManager == null) {
            ducked = false;
            originalVolume = -1;
            return;
        }

        try {
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (originalVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                FileLogger.log("CameraVolumeController: restored volume " + current + " -> " + originalVolume);
            }
        } catch (Throwable t) {
            FileLogger.log("CameraVolumeController: error on restore: " + t.getMessage());
        } finally {
            ducked = false;
            originalVolume = -1;
        }
    }

    private void resetTimeoutTimer() {
        cancelTimeoutTimer();
        timeoutTimer = new Timer("AutoOk-CamDuckTimeout");
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (CameraVolumeController.this) {
                    if (ducked) {
                        FileLogger.log("CameraVolumeController: watchdog timeout (30s) reached, forcing restore");
                        onCameraExit();
                    }
                }
            }
        }, AutoOkConfig.REVERSE_CAMERA_DUCK_TIMEOUT_MS);
    }

    private void cancelTimeoutTimer() {
        if (timeoutTimer != null) {
            try {
                timeoutTimer.cancel();
            } catch (Throwable ignored) {
            }
            timeoutTimer = null;
        }
    }

    public synchronized void destroy() {
        cancelTimeoutTimer();
        if (ducked) {
            onCameraExit();
        }
    }

    public static int calculateDuckedVolume(int currentVolume, int duckSteps) {
        if (currentVolume < 0) {
            return 0;
        }
        return Math.max(0, currentVolume - duckSteps);
    }
}
