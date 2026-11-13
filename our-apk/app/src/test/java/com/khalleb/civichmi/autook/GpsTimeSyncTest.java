package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.TimeZone;

public class GpsTimeSyncTest {

    @Test
    public void deveDetectarDriftSignificativo_quandoAcimaDoThreshold() {
        long now = 1700000000000L; // ms
        long gpsDriftAhead = now + 6000L;
        long gpsDriftBehind = now - 6000L;

        assertTrue(GpsTimeSync.isSignificantDrift(gpsDriftAhead, now, 5L));
        assertTrue(GpsTimeSync.isSignificantDrift(gpsDriftBehind, now, 5L));
    }

    @Test
    public void naoDeveDetectarDrift_quandoDentroDoThreshold() {
        long now = 1700000000000L;
        long gpsSmallDrift = now + 2000L;

        assertFalse(GpsTimeSync.isSignificantDrift(gpsSmallDrift, now, 5L));
        assertFalse(GpsTimeSync.isSignificantDrift(now, now, 5L));
    }

    @Test
    public void deveFormatarDataCorretamenteParaComandoDate() {
        // 2026-11-13 12:30:45 UTC = 1794573045000L
        long epochMs = 1794573045000L;
        String formatted = GpsTimeSync.formatDateForDateCommand(epochMs, TimeZone.getTimeZone("UTC"));
        assertEquals("20261113.123045", formatted);
    }
}
