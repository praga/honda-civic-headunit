package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutoAppLauncherTest {

    @Test
    public void deveLancar_quandoHabilitadoEPacoteValido() {
        assertTrue(AutoAppLauncher.shouldLaunch(true, "com.spotify.music"));
        assertTrue(AutoAppLauncher.shouldLaunch(true, "com.waze"));
    }

    @Test
    public void naoDeveLancar_quandoDesabilitado() {
        assertFalse(AutoAppLauncher.shouldLaunch(false, "com.spotify.music"));
    }

    @Test
    public void naoDeveLancar_quandoPacoteNuloOuVazio() {
        assertFalse(AutoAppLauncher.shouldLaunch(true, null));
        assertFalse(AutoAppLauncher.shouldLaunch(true, ""));
        assertFalse(AutoAppLauncher.shouldLaunch(true, "   "));
    }
}
