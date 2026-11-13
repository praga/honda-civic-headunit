package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
