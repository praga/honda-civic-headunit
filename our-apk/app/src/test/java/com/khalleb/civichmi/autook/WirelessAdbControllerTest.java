package com.khalleb.civichmi.autook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WirelessAdbControllerTest {

    private static class MockShellRunner implements WirelessAdbController.ShellRunner {
        String persistPort = "";
        String servicePort = "";
        final List<String> executedCommands = new ArrayList<String>();
        boolean suResult = true;

        @Override
        public boolean su(String command) {
            executedCommands.add(command);
            return suResult;
        }

        @Override
        public String suExecCapture(String command) {
            if ("getprop service.adb.tcp.port".equals(command)) {
                return servicePort;
            }
            if ("getprop persist.adb.tcp.port".equals(command)) {
                return persistPort;
            }
            return "";
        }
    }

    private static class TestableWirelessAdbController extends WirelessAdbController {
        String simulatedIp;
        final List<String> loggedMessages = new ArrayList<String>();

        TestableWirelessAdbController(MockShellRunner runner) {
            super(null, 5555, runner);
        }

        @Override
        protected String resolveIp() {
            return simulatedIp;
        }

        @Override
        protected void log(String msg) {
            loggedMessages.add(msg);
        }
    }

    @Test
    public void isPortConfigured_validaPortasCorretamente() {
        assertTrue(WirelessAdbController.isPortConfigured("5555", 5555));
        assertTrue(WirelessAdbController.isPortConfigured(" 5555 \n", 5555));
        assertFalse(WirelessAdbController.isPortConfigured("5554", 5555));
        assertFalse(WirelessAdbController.isPortConfigured("-1", 5555));
        assertFalse(WirelessAdbController.isPortConfigured("", 5555));
        assertFalse(WirelessAdbController.isPortConfigured("   ", 5555));
        assertFalse(WirelessAdbController.isPortConfigured(null, 5555));
    }

    @Test
    public void isAdbTcpEnabled_requerAmbasPropriedades() {
        assertTrue(WirelessAdbController.isAdbTcpEnabled("5555", "5555", 5555));
        assertFalse(WirelessAdbController.isAdbTcpEnabled("5555", "-1", 5555));
        assertFalse(WirelessAdbController.isAdbTcpEnabled("", "5555", 5555));
        assertFalse(WirelessAdbController.isAdbTcpEnabled(null, "5555", 5555));
        assertFalse(WirelessAdbController.isAdbTcpEnabled("5555", null, 5555));
        assertFalse(WirelessAdbController.isAdbTcpEnabled(null, null, 5555));
    }

    @Test
    public void buildEnableAdbCommand_contemComandosNecessarios() {
        String cmd = WirelessAdbController.buildEnableAdbCommand(5555);
        assertTrue(cmd.contains("setprop persist.adb.tcp.port 5555"));
        assertTrue(cmd.contains("setprop service.adb.tcp.port 5555"));
        assertTrue(cmd.contains("stop adbd"));
        assertTrue(cmd.contains("start adbd"));
    }

    @Test
    public void formatIpAddress_formataLittleEndianCorretamente() {
        assertNull(WirelessAdbController.formatIpAddress(0));

        // 192.168.1.100 em little endian:
        // byte0=192 (0xC0), byte1=168 (0xA8), byte2=1 (0x01), byte3=100 (0x64)
        int ipInt = (192) | (168 << 8) | (1 << 16) | (100 << 24);
        assertEquals("192.168.1.100", WirelessAdbController.formatIpAddress(ipInt));

        // 127.0.0.1 em little endian:
        int localhost = (127) | (0 << 8) | (0 << 16) | (1 << 24);
        assertEquals("127.0.0.1", WirelessAdbController.formatIpAddress(localhost));

        // 10.0.0.1
        int classA = (10) | (0 << 8) | (0 << 16) | (1 << 24);
        assertEquals("10.0.0.1", WirelessAdbController.formatIpAddress(classA));
    }

    @Test
    public void shouldReportIp_detectaMudancasDeIp() {
        assertTrue(WirelessAdbController.shouldReportIp("192.168.1.50", null));
        assertTrue(WirelessAdbController.shouldReportIp("192.168.43.100", "192.168.1.50"));
        assertFalse(WirelessAdbController.shouldReportIp("192.168.1.50", "192.168.1.50"));
        assertFalse(WirelessAdbController.shouldReportIp(null, "192.168.1.50"));
        assertFalse(WirelessAdbController.shouldReportIp("", "192.168.1.50"));
    }

    @Test
    public void checkAndConfigureAdb_configuraPortaQuandoNaoConfigurada() {
        MockShellRunner runner = new MockShellRunner();
        runner.persistPort = "";
        runner.servicePort = "-1";

        TestableWirelessAdbController controller = new TestableWirelessAdbController(runner);
        controller.simulatedIp = "192.168.1.150";

        controller.checkAndConfigureAdb();

        // Deve ter executado os comandos para setar as propriedades e reiniciar adbd
        assertEquals(1, runner.executedCommands.size());
        String executed = runner.executedCommands.get(0);
        assertTrue(executed.contains("setprop persist.adb.tcp.port 5555"));
        assertTrue(executed.contains("stop adbd"));
        assertTrue(executed.contains("start adbd"));

        assertEquals("192.168.1.150", controller.getLastReportedIp());
        boolean hasExpectedLog = false;
        for (String msg : controller.loggedMessages) {
            if (msg.contains("Wireless ADB enabled at 192.168.1.150:5555")) {
                hasExpectedLog = true;
                break;
            }
        }
        assertTrue("Deve logar que Wireless ADB foi ativado com IP e porta", hasExpectedLog);
    }

    @Test
    public void checkAndConfigureAdb_naoReconfiguraQuandoJaHabilitado() {
        MockShellRunner runner = new MockShellRunner();
        runner.persistPort = "5555";
        runner.servicePort = "5555";

        TestableWirelessAdbController controller = new TestableWirelessAdbController(runner);
        controller.simulatedIp = "192.168.1.150";

        controller.checkAndConfigureAdb();

        // Não deve executar comandos no shell se já está 5555
        assertEquals(0, runner.executedCommands.size());
        assertEquals("192.168.1.150", controller.getLastReportedIp());
    }

    @Test
    public void checkAndConfigureAdb_trataTransicaoDeDesconexaoEConexao() {
        MockShellRunner runner = new MockShellRunner();
        runner.persistPort = "5555";
        runner.servicePort = "5555";

        TestableWirelessAdbController controller = new TestableWirelessAdbController(runner);

        // 1. Conecta no Wi-Fi
        controller.simulatedIp = "192.168.1.150";
        controller.checkAndConfigureAdb();
        assertEquals("192.168.1.150", controller.getLastReportedIp());

        // 2. Mesma verificação, não repete relatório
        controller.loggedMessages.clear();
        controller.checkAndConfigureAdb();
        assertEquals(0, controller.loggedMessages.size());

        // 3. Desconecta do Wi-Fi
        controller.simulatedIp = null;
        controller.checkAndConfigureAdb();
        assertNull(controller.getLastReportedIp());
        boolean hasDisconnectLog = false;
        for (String msg : controller.loggedMessages) {
            if (msg.contains("Wi-Fi disconnected")) {
                hasDisconnectLog = true;
                break;
            }
        }
        assertTrue("Deve logar desconexão do Wi-Fi", hasDisconnectLog);

        // 4. Reconecta em outro hotspot
        controller.loggedMessages.clear();
        controller.simulatedIp = "10.0.0.45";
        controller.checkAndConfigureAdb();
        assertEquals("10.0.0.45", controller.getLastReportedIp());
        boolean hasNewLog = false;
        for (String msg : controller.loggedMessages) {
            if (msg.contains("Wireless ADB enabled at 10.0.0.45:5555")) {
                hasNewLog = true;
                break;
            }
        }
        assertTrue("Deve logar reconexão com novo IP", hasNewLog);
    }

    @Test
    public void startStop_gerenciaCicloDeVidaCorretamente() {
        MockShellRunner runner = new MockShellRunner();
        runner.persistPort = "5555";
        runner.servicePort = "5555";

        WirelessAdbController controller = new WirelessAdbController(null, 5555, runner);
        assertFalse(controller.isStarted());

        controller.start();
        assertTrue(controller.isStarted());

        controller.stop();
        assertFalse(controller.isStarted());
        assertNull(controller.getLastReportedIp());
    }
}
