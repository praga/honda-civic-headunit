package com.khalleb.civichmi.autook;

public final class CameraActivityMatcher {

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
