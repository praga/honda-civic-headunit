package com.khalleb.civichmi.autook;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GpsTimeSync {

    private final Context context;
    private final LocationManager locationManager;
    private LocationListener locationListener;
    private volatile boolean listening;
    private volatile long lastSyncTimeMs;
    private volatile boolean hasSynced;

    public GpsTimeSync(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public synchronized void start() {
        if (!AutoOkConfig.ENABLE_GPS_TIME_SYNC) {
            return;
        }
        if (listening || locationManager == null) {
            return;
        }

        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                FileLogger.log("GpsTimeSync: GPS_PROVIDER disabled on system");
                return;
            }

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    onGpsLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };

            // Request updates every 5 seconds, min distance 0
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                0.0f,
                locationListener,
                Looper.getMainLooper()
            );
            listening = true;
            FileLogger.log("GpsTimeSync: listening for GPS fix");
        } catch (SecurityException e) {
            FileLogger.log("GpsTimeSync: missing ACCESS_FINE_LOCATION permission: " + e.getMessage());
        } catch (Throwable t) {
            FileLogger.log("GpsTimeSync: error starting listener: " + t.getMessage());
        }
    }

    public synchronized void stop() {
        if (!listening) {
            return;
        }
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Throwable ignored) {
            }
        }
        listening = false;
        locationListener = null;
    }

    public void onGpsLocation(Location location) {
        if (location == null) {
            return;
        }
        long gpsTimeMs = location.getTime();
        if (gpsTimeMs <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long driftSec = Math.abs(gpsTimeMs - now) / 1000L;

        if (hasSynced && (now - lastSyncTimeMs < AutoOkConfig.GPS_TIME_SYNC_MIN_INTERVAL_MS)) {
            return;
        }

        if (isSignificantDrift(gpsTimeMs, now, AutoOkConfig.GPS_TIME_SYNC_MAX_DRIFT_SEC)) {
            syncClock(gpsTimeMs, driftSec);
        }
    }

    private synchronized void syncClock(final long gpsTimeMs, final long driftSec) {
        lastSyncTimeMs = System.currentTimeMillis();
        hasSynced = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                String cmdDate = formatDateForDateCommand(gpsTimeMs, TimeZone.getDefault());
                FileLogger.log("GpsTimeSync: drift=" + driftSec + "s -> setting system clock to " + cmdDate);
                boolean okDate = Shell.su("date -s " + cmdDate);
                Shell.su("busybox hwclock -w");
                FileLogger.log("GpsTimeSync: date -s result ok=" + okDate);
            }
        }, "AutoOk-GpsTimeSyncWorker").start();
    }

    public static boolean isSignificantDrift(long gpsTimeMs, long systemTimeMs, long thresholdSec) {
        long deltaMs = Math.abs(gpsTimeMs - systemTimeMs);
        return deltaMs >= (thresholdSec * 1000L);
    }

    public static String formatDateForDateCommand(long timeMs, TimeZone tz) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.US);
        if (tz != null) {
            sdf.setTimeZone(tz);
        }
        return sdf.format(new Date(timeMs));
    }
}
