package com.khalleb.civichmi.autook;

import android.content.Context;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraVolumeController {

    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_VOLUME_DOWN = 25;

    private final Context context;
    private final ExecutorService executor;
    private volatile boolean ducked;
    private volatile int duckedStepsApplied;
    private Timer timeoutTimer;

    public CameraVolumeController(Context context) {
        this(context, Executors.newSingleThreadExecutor());
    }

    public CameraVolumeController(Context context, ExecutorService executor) {
        this.context = context;
        this.executor = executor;
    }

    public synchronized boolean isDucked() {
        return ducked;
    }

    public synchronized int getOriginalVolume() {
        return duckedStepsApplied;
    }

    public synchronized int getDuckedStepsApplied() {
        return duckedStepsApplied;
    }

    public synchronized void onCameraEnter() {
        if (!AutoOkConfig.ENABLE_REVERSE_CAMERA_DUCK) {
            return;
        }
        if (ducked) {
            resetTimeoutTimer();
            return;
        }

        final int steps = AutoOkConfig.REVERSE_CAMERA_DUCK_STEPS;
        ducked = true;
        duckedStepsApplied = steps;
        resetTimeoutTimer();
        FileLogger.log("CameraVolumeController: onCameraEnter - stepping down master volume (" + steps + " steps)");

        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendVolumeKeyevents(KEYCODE_VOLUME_DOWN, steps);
                }
            });
        }
    }

    public synchronized void onCameraExit() {
        if (!ducked) {
            return;
        }
        cancelTimeoutTimer();
        final int steps = duckedStepsApplied;
        ducked = false;
        duckedStepsApplied = 0;
        FileLogger.log("CameraVolumeController: onCameraExit - restoring master volume (" + steps + " steps)");

        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendVolumeKeyevents(KEYCODE_VOLUME_UP, steps);
                }
            });
        }
    }

    public static String buildVolumeKeyeventsScript(int keyCode, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("input keyevent ").append(keyCode).append("\n");
        }
        return sb.toString();
    }

    public static void sendVolumeKeyevents(int keyCode, int count) {
        String script = buildVolumeKeyeventsScript(keyCode, count);
        if (script.length() > 0) {
            Shell.suExec(script);
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
                        FileLogger.log("CameraVolumeController: watchdog timeout reached, forcing restore");
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
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (Throwable ignored) {
            }
        }
    }

    public static int calculateDuckedVolume(int currentVolume, int duckSteps) {
        if (currentVolume < 0) {
            return 0;
        }
        return Math.max(0, currentVolume - duckSteps);
    }
}
