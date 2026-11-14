package com.khalleb.civichmi.autook;

public final class CameraActivityMatcher {

    public static final String EVENT_FINISH = "am_finish_activity";

    private CameraActivityMatcher() {
    }

    public static boolean isCameraActivity(String line) {
        if (line == null) {
            return false;
        }
        if (!isActivityEvent(line)) {
            return false;
        }
        return line.contains(AutoOkConfig.CAMERA_PACKAGE_PREFIX);
    }

    public static boolean isCameraFinish(String line) {
        if (line == null) {
            return false;
        }
        return line.contains(EVENT_FINISH) && line.contains(AutoOkConfig.CAMERA_PACKAGE_PREFIX);
    }

    public static boolean isNonCameraActivity(String line) {
        if (line == null) {
            return false;
        }
        if (!isActivityEvent(line)) {
            return false;
        }
        return !line.contains(AutoOkConfig.CAMERA_PACKAGE_PREFIX);
    }

    private static boolean isActivityEvent(String line) {
        return line.contains(AvisoMatcher.EVENT_CREATE) || line.contains(AvisoMatcher.EVENT_NEW_INTENT);
    }
}
