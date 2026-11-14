package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraVolumeControllerTest {

    @Test
    public void deveCalcularDuckedVolume_reduzPassosCorretamente() {
        assertEquals(9, CameraVolumeController.calculateDuckedVolume(15, 6));
        assertEquals(4, CameraVolumeController.calculateDuckedVolume(10, 6));
    }

    @Test
    public void deveLimitarVolumeMinimoEmZero() {
        assertEquals(0, CameraVolumeController.calculateDuckedVolume(4, 6));
        assertEquals(0, CameraVolumeController.calculateDuckedVolume(0, 6));
        assertEquals(0, CameraVolumeController.calculateDuckedVolume(-5, 6));
    }

    @Test
    public void deveGerarScriptDeVolumeKeyevents() {
        String scriptDown = CameraVolumeController.buildVolumeKeyeventsScript(CameraVolumeController.KEYCODE_VOLUME_DOWN, 3);
        assertEquals("input keyevent 25\ninput keyevent 25\ninput keyevent 25\n", scriptDown);

        String scriptUp = CameraVolumeController.buildVolumeKeyeventsScript(CameraVolumeController.KEYCODE_VOLUME_UP, 2);
        assertEquals("input keyevent 24\ninput keyevent 24\n", scriptUp);

        String scriptZero = CameraVolumeController.buildVolumeKeyeventsScript(CameraVolumeController.KEYCODE_VOLUME_DOWN, 0);
        assertEquals("", scriptZero);
    }

    @Test
    public void deveControlarEstadoDuckedCorretamente() {
        ExecutorService directExecutor = Executors.newSingleThreadExecutor();
        CameraVolumeController controller = new CameraVolumeController(null, directExecutor);

        assertFalse(controller.isDucked());
        assertEquals(0, controller.getDuckedStepsApplied());

        controller.onCameraEnter();
        assertTrue(controller.isDucked());
        assertEquals(AutoOkConfig.REVERSE_CAMERA_DUCK_STEPS, controller.getDuckedStepsApplied());

        // Segundo enter nao duplica
        controller.onCameraEnter();
        assertTrue(controller.isDucked());

        controller.onCameraExit();
        assertFalse(controller.isDucked());
        assertEquals(0, controller.getDuckedStepsApplied());

        controller.destroy();
    }
}
