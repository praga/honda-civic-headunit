package com.khalleb.civichmi.autook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Listens for newly installed or updated applications and dynamically
 * applies driving restriction bypass to whitelist them immediately.
 */
public class PackageInstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            Uri data = intent.getData();
            String packageName = data != null ? data.getSchemeSpecificPart() : null;
            if (packageName != null && !packageName.isEmpty()) {
                DrivingRestrictionBypass.onPackageInstalled(context, packageName);
            }
        }
    }
}
