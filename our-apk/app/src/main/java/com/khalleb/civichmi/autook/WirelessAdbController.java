package com.khalleb.civichmi.autook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WirelessAdbController {

    public interface ShellRunner {
        boolean su(String command);
        String suExecCapture(String command);
    }

    private static final ShellRunner DEFAULT_SHELL_RUNNER = new ShellRunner() {
        @Override
        public boolean su(String command) {
            return Shell.su(command);
        }

        @Override
        public String suExecCapture(String command) {
            return Shell.suExecCapture(command);
        }
    };

    private final Context context;
    private final int port;
    private final ShellRunner shellRunner;

    private volatile boolean started;
    private volatile String lastReportedIp;
    private BroadcastReceiver networkReceiver;
    private ExecutorService executor;

    public WirelessAdbController(Context context) {
        this(context, AutoOkConfig.WIRELESS_ADB_PORT, DEFAULT_SHELL_RUNNER);
    }

    public WirelessAdbController(Context context, int port) {
        this(context, port, DEFAULT_SHELL_RUNNER);
    }

    public WirelessAdbController(Context context, int port, ShellRunner shellRunner) {
        this.context = context;
        this.port = port;
        this.shellRunner = shellRunner != null ? shellRunner : DEFAULT_SHELL_RUNNER;
    }

    public synchronized void start() {
        if (!AutoOkConfig.ENABLE_WIRELESS_ADB) {
            return;
        }
        if (started) {
            return;
        }
        started = true;
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
        registerNetworkReceiver();
        triggerAdbCheckAsync();
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        unregisterNetworkReceiver();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        lastReportedIp = null;
    }

    public boolean isStarted() {
        return started;
    }

    public int getPort() {
        return port;
    }

    public String getLastReportedIp() {
        return lastReportedIp;
    }

    public void triggerAdbCheckAsync() {
        ExecutorService exec = executor;
        if (exec != null && !exec.isShutdown()) {
            try {
                exec.submit(new Runnable() {
                    @Override
                    public void run() {
                        checkAndConfigureAdb();
                    }
                });
            } catch (Throwable ignored) {
            }
        }
    }

    public void onNetworkChanged() {
        triggerAdbCheckAsync();
    }

    public synchronized void checkAndConfigureAdb() {
        try {
            String servicePort = shellRunner.suExecCapture("getprop service.adb.tcp.port");
            String persistPort = shellRunner.suExecCapture("getprop persist.adb.tcp.port");
            if (!isAdbTcpEnabled(persistPort, servicePort, port)) {
                String sPort = servicePort != null ? servicePort.trim() : "";
                String pPort = persistPort != null ? persistPort.trim() : "";
                log("WirelessAdb: configuring port " + port + " (persist=" + pPort + ", service=" + sPort + ")");
                String cmd = buildEnableAdbCommand(port);
                boolean ok = shellRunner.su(cmd);
                log("WirelessAdb: adbd restart ok=" + ok);
            }

            String ip = resolveIp();
            if (shouldReportIp(ip, lastReportedIp)) {
                lastReportedIp = ip;
                log("Wireless ADB enabled at " + ip + ":" + port);
            } else if ((ip == null || ip.isEmpty()) && lastReportedIp != null) {
                log("Wireless ADB: Wi-Fi disconnected");
                lastReportedIp = null;
            }
        } catch (Throwable t) {
            log("WirelessAdb: check error: " + t.getMessage());
        }
    }

    protected String resolveIp() {
        return getWifiIpAddress(context);
    }

    protected void log(String msg) {
        try {
            FileLogger.log(msg);
        } catch (Throwable ignored) {
        }
    }

    private synchronized void registerNetworkReceiver() {
        if (context == null || networkReceiver != null) {
            return;
        }
        try {
            networkReceiver = new NetworkChangeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            context.registerReceiver(networkReceiver, filter);
        } catch (Throwable t) {
            log("WirelessAdb: registerReceiver failed: " + t.getMessage());
        }
    }

    private synchronized void unregisterNetworkReceiver() {
        if (context == null || networkReceiver == null) {
            return;
        }
        try {
            context.unregisterReceiver(networkReceiver);
        } catch (Throwable ignored) {
        }
        networkReceiver = null;
    }

    private final class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action) ||
                ConnectivityManager.CONNECTIVITY_ACTION.equals(action) ||
                WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                onNetworkChanged();
            }
        }
    }

    public static boolean isPortConfigured(String propValue, int expectedPort) {
        if (propValue == null) {
            return false;
        }
        return String.valueOf(expectedPort).equals(propValue.trim());
    }

    public static boolean isAdbTcpEnabled(String persistPort, String servicePort, int expectedPort) {
        return isPortConfigured(persistPort, expectedPort) && isPortConfigured(servicePort, expectedPort);
    }

    public static String buildEnableAdbCommand(int port) {
        return "setprop persist.adb.tcp.port " + port + "\n"
             + "setprop service.adb.tcp.port " + port + "\n"
             + "stop adbd\n"
             + "start adbd";
    }

    public static boolean shouldReportIp(String currentIp, String lastReportedIp) {
        if (currentIp == null || currentIp.isEmpty()) {
            return false;
        }
        return !currentIp.equals(lastReportedIp);
    }

    public static String formatIpAddress(int ipInt) {
        if (ipInt == 0) {
            return null;
        }
        return String.format(Locale.US, "%d.%d.%d.%d",
                (ipInt & 0xff),
                ((ipInt >>> 8) & 0xff),
                ((ipInt >>> 16) & 0xff),
                ((ipInt >>> 24) & 0xff));
    }

    public static String getWifiIpAddress(Context context) {
        if (context != null) {
            try {
                Context appCtx = context.getApplicationContext() != null ? context.getApplicationContext() : context;
                WifiManager wifiManager = (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    if (info != null) {
                        int ipInt = info.getIpAddress();
                        if (ipInt != 0) {
                            String formatted = formatIpAddress(ipInt);
                            if (formatted != null && !formatted.isEmpty() && !"0.0.0.0".equals(formatted)) {
                                return formatted;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return getIpFromNetworkInterfaces();
    }

    public static String getIpFromNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return null;
            }
            List<NetworkInterface> list = Collections.list(interfaces);
            // 1. Prefer wlan, ap, eth, or rndis interfaces
            for (NetworkInterface intf : list) {
                String name = intf.getName();
                if (name != null && (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("eth") || name.startsWith("rndis"))) {
                    String ip = extractIpv4(intf);
                    if (ip != null) {
                        return ip;
                    }
                }
            }
            // 2. Fallback to any non-loopback interface
            for (NetworkInterface intf : list) {
                String name = intf.getName();
                if (name != null && !name.startsWith("lo") && !name.startsWith("dummy")) {
                    String ip = extractIpv4(intf);
                    if (ip != null) {
                        return ip;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String extractIpv4(NetworkInterface intf) {
        if (intf == null) {
            return null;
        }
        try {
            Enumeration<InetAddress> addrs = intf.getInetAddresses();
            if (addrs == null) {
                return null;
            }
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                    String hostAddress = addr.getHostAddress();
                    if (hostAddress != null && !hostAddress.isEmpty() && !"0.0.0.0".equals(hostAddress)) {
                        return hostAddress;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
