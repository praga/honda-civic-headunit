package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AudioMuteGuardTest {

    @Test
    public void naoDeveRecuperar_quandoCameraEstaDucked() {
        assertFalse(AudioMuteGuard.shouldRecover(true, 0, 5, 2));
    }

    @Test
    public void naoDeveRecuperar_quandoVolumeMaiorQueZero() {
        assertFalse(AudioMuteGuard.shouldRecover(false, 10, 5, 2));
        assertFalse(AudioMuteGuard.shouldRecover(false, 1, 5, 2));
    }

    @Test
    public void naoDeveRecuperar_quandoContadorAbaixoDoThreshold() {
        assertFalse(AudioMuteGuard.shouldRecover(false, 0, 0, 2));
        assertFalse(AudioMuteGuard.shouldRecover(false, 0, 1, 2));
    }

    @Test
    public void deveRecuperar_quandoVolumeZeroEMudoPersistiu() {
        assertTrue(AudioMuteGuard.shouldRecover(false, 0, 2, 2));
        assertTrue(AudioMuteGuard.shouldRecover(false, 0, 3, 2));
    }
}
