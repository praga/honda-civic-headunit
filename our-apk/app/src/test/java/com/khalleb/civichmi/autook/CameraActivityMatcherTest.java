package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CameraActivityMatcherTest {

    private static final String CAMERA_LINE_CREATE =
        "10-25 08:00:01.123  1200  1200 I am_create_activity: [0,999,com.mitsubishielectric.ada.app.camera/.RearWideCameraDisplayActivity,android.intent.action.MAIN]";

    private static final String CAMERA_LINE_NEW_INTENT =
        "10-25 08:00:01.123  1200  1200 I am_new_intent: [0,999,com.mitsubishielectric.ada.app.camera/.MvcDisplayActivity,android.intent.action.MAIN]";

    private static final String LAUNCHER_LINE =
        "10-25 08:00:05.123  1200  1200 I am_create_activity: [0,999,com.mitsubishielectric.ada.app.dalauncher/.LauncherActivity,android.intent.action.MAIN]";

    private static final String SPOTIFY_LINE =
        "10-25 08:00:10.123  1200  1200 I am_new_intent: [0,999,com.spotify.music/.MainActivity,android.intent.action.MAIN]";

    private static final String CAMERA_LINE_FINISH =
        "2026-11-14 08:59:55.010   947  1356 I am_finish_activity: [0,1098928600,244,com.mitsubishielectric.ada.app.camera/.activity.RearWideCameraDisplayActivity,app-request]";

    @Test
    public void deveDetectar_cameraActivity_amCreate() {
        assertTrue(CameraActivityMatcher.isCameraActivity(CAMERA_LINE_CREATE));
        assertFalse(CameraActivityMatcher.isNonCameraActivity(CAMERA_LINE_CREATE));
        assertFalse(CameraActivityMatcher.isCameraFinish(CAMERA_LINE_CREATE));
    }

    @Test
    public void deveDetectar_cameraActivity_amNewIntent() {
        assertTrue(CameraActivityMatcher.isCameraActivity(CAMERA_LINE_NEW_INTENT));
        assertFalse(CameraActivityMatcher.isNonCameraActivity(CAMERA_LINE_NEW_INTENT));
        assertFalse(CameraActivityMatcher.isCameraFinish(CAMERA_LINE_NEW_INTENT));
    }

    @Test
    public void deveDetectar_cameraActivity_amFinish() {
        assertTrue(CameraActivityMatcher.isCameraFinish(CAMERA_LINE_FINISH));
        assertFalse(CameraActivityMatcher.isCameraActivity(CAMERA_LINE_FINISH));
        assertFalse(CameraActivityMatcher.isNonCameraActivity(CAMERA_LINE_FINISH));
    }

    @Test
    public void deveDetectar_nonCameraActivity_launcherOuApps() {
        assertFalse(CameraActivityMatcher.isCameraActivity(LAUNCHER_LINE));
        assertTrue(CameraActivityMatcher.isNonCameraActivity(LAUNCHER_LINE));
        assertFalse(CameraActivityMatcher.isCameraFinish(LAUNCHER_LINE));

        assertFalse(CameraActivityMatcher.isCameraActivity(SPOTIFY_LINE));
        assertTrue(CameraActivityMatcher.isNonCameraActivity(SPOTIFY_LINE));
        assertFalse(CameraActivityMatcher.isCameraFinish(SPOTIFY_LINE));
    }

    @Test
    public void naoDeveDetectar_linhaIrrelevanteOuDestroy() {
        String destroyLine = "10-25 08:00:01.123  1200  1200 I am_destroy_activity: [0,999,com.mitsubishielectric.ada.app.camera/.RearWideCameraDisplayActivity]";
        assertFalse(CameraActivityMatcher.isCameraActivity(destroyLine));
        assertFalse(CameraActivityMatcher.isNonCameraActivity(destroyLine));
        assertFalse(CameraActivityMatcher.isCameraFinish(destroyLine));

        assertFalse(CameraActivityMatcher.isCameraActivity("algum log comum"));
        assertFalse(CameraActivityMatcher.isNonCameraActivity("algum log comum"));
        assertFalse(CameraActivityMatcher.isCameraFinish("algum log comum"));
    }

    @Test
    public void naoDeveFalharComNull() {
        assertFalse(CameraActivityMatcher.isCameraActivity(null));
        assertFalse(CameraActivityMatcher.isNonCameraActivity(null));
        assertFalse(CameraActivityMatcher.isCameraFinish(null));
    }
}
