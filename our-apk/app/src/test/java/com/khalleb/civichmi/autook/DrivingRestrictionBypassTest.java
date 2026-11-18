package com.khalleb.civichmi.autook;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DrivingRestrictionBypassTest {

    private static final String STOCK_MITSUBISHI_XML_SAMPLE =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<whiteList>\n" +
        "    <formatVersion>1.0</formatVersion>\n" +
        "    <contentVersion>1.5</contentVersion>\n" +
        "    <applicationLists>\n" +
        "        <application>\n" +
        "            <property>\n" +
        "                <name>Clock</name>\n" +
        "                <package>com.android.deskclock</package>\n" +
        "                <versionCode>1-999999999</versionCode>\n" +
        "                <keyStoreLists>\n" +
        "                    <keyStore>null</keyStore>\n" +
        "                </keyStoreLists>\n" +
        "            </property>\n" +
        "            <controlData>\n" +
        "                <withAudio>without</withAudio>\n" +
        "                <audioStreamType>null</audioStreamType>\n" +
        "                <regulation>speedtrigger</regulation>\n" +
        "                <revert>no</revert>\n" +
        "            </controlData>\n" +
        "        </application>\n" +
        "        <application>\n" +
        "            <property>\n" +
        "                <name>Browser</name>\n" +
        "                <package>com.android.browser</package>\n" +
        "                <versionCode>1-999999999</versionCode>\n" +
        "                <keyStoreLists>\n" +
        "                    <keyStore>null</keyStore>\n" +
        "                </keyStoreLists>\n" +
        "            </property>\n" +
        "            <controlData>\n" +
        "                <withAudio>with</withAudio>\n" +
        "                <audioStreamType>null</audioStreamType>\n" +
        "                <regulation>park&amp;speedtrigger</regulation>\n" +
        "                <revert>no</revert>\n" +
        "            </controlData>\n" +
        "        </application>\n" +
        "        <application>\n" +
        "            <property>\n" +
        "                <name>Settings</name>\n" +
        "                <package>com.android.settings</package>\n" +
        "                <versionCode>1-999999999</versionCode>\n" +
        "                <keyStoreLists>\n" +
        "                    <keyStore>null</keyStore>\n" +
        "                </keyStoreLists>\n" +
        "            </property>\n" +
        "            <controlData>\n" +
        "                <withAudio>without</withAudio>\n" +
        "                <audioStreamType>null</audioStreamType>\n" +
        "                <regulation>parktrigger</regulation>\n" +
        "                <revert>no</revert>\n" +
        "            </controlData>\n" +
        "        </application>\n" +
        "    </applicationLists>\n" +
        "</whiteList>";

    @Test
    public void testExtractPackageNames() {
        Set<String> pkgs = DrivingRestrictionBypass.extractPackageNames(STOCK_MITSUBISHI_XML_SAMPLE);
        assertEquals(3, pkgs.size());
        assertTrue(pkgs.contains("com.android.deskclock"));
        assertTrue(pkgs.contains("com.android.browser"));
        assertTrue(pkgs.contains("com.android.settings"));
        assertFalse(pkgs.contains("com.waze"));
    }

    @Test
    public void testExtractPackageNamesEmptyOrNull() {
        assertTrue(DrivingRestrictionBypass.extractPackageNames(null).isEmpty());
        assertTrue(DrivingRestrictionBypass.extractPackageNames("").isEmpty());
        assertTrue(DrivingRestrictionBypass.extractPackageNames("<invalid>xml</invalid>").isEmpty());
    }

    @Test
    public void testIsBypassNeededDetectsTriggers() {
        // Stock XML has speedtrigger, parktrigger, park&speedtrigger -> needs bypass
        assertTrue(DrivingRestrictionBypass.isBypassNeeded(STOCK_MITSUBISHI_XML_SAMPLE, null));

        // When missing required package
        List<String> required = Collections.singletonList("com.waze");
        assertTrue(DrivingRestrictionBypass.isBypassNeeded(STOCK_MITSUBISHI_XML_SAMPLE, required));
    }

    @Test
    public void testSanitizeWhitelistXmlReplacesAllRegulationsWithNull() {
        String sanitized = DrivingRestrictionBypass.sanitizeWhitelistXml(STOCK_MITSUBISHI_XML_SAMPLE, null);

        assertNotNull(sanitized);
        assertFalse(sanitized.contains("speedtrigger"));
        assertFalse(sanitized.contains("parktrigger"));
        assertFalse(sanitized.contains("park&amp;speedtrigger"));

        // Count occurrences of <regulation>null</regulation>
        int count = 0;
        int idx = 0;
        while ((idx = sanitized.indexOf("<regulation>null</regulation>", idx)) != -1) {
            count++;
            idx += "<regulation>null</regulation>".length();
        }
        assertEquals(3, count);
    }

    @Test
    public void testSanitizeWhitelistXmlAppendsMissingPackages() {
        List<String> extraPackages = Arrays.asList("com.waze", "com.spotify.music", "com.android.browser");
        String sanitized = DrivingRestrictionBypass.sanitizeWhitelistXml(STOCK_MITSUBISHI_XML_SAMPLE, extraPackages);

        Set<String> pkgs = DrivingRestrictionBypass.extractPackageNames(sanitized);
        assertEquals(5, pkgs.size());
        assertTrue(pkgs.contains("com.android.deskclock"));
        assertTrue(pkgs.contains("com.android.browser")); // existing, not duplicated
        assertTrue(pkgs.contains("com.android.settings"));
        assertTrue(pkgs.contains("com.waze"));            // newly added
        assertTrue(pkgs.contains("com.spotify.music"));    // newly added

        // Check that added packages have regulation=null
        assertTrue(sanitized.contains("<name>com.waze</name>"));
        assertTrue(sanitized.contains("<package>com.waze</package>"));
        assertTrue(sanitized.contains("<name>com.spotify.music</name>"));

        // Bypass should now report false (no changes needed)
        assertFalse(DrivingRestrictionBypass.isBypassNeeded(sanitized, extraPackages));
    }

    @Test
    public void testBuildDefaultWhitelist() {
        List<String> extra = Collections.singletonList("com.custom.navigation");
        String xml = DrivingRestrictionBypass.buildDefaultWhitelist(extra);

        assertNotNull(xml);
        assertTrue(xml.contains("<whiteList>"));
        assertTrue(xml.contains("</whiteList>"));
        assertTrue(xml.contains("<package>com.custom.navigation</package>"));
        assertTrue(xml.contains("<regulation>null</regulation>"));
        assertFalse(xml.contains("speedtrigger"));
        assertFalse(xml.contains("parktrigger"));

        // All default packages should be present
        Set<String> pkgs = DrivingRestrictionBypass.extractPackageNames(xml);
        assertTrue(pkgs.contains("com.custom.navigation"));
        assertTrue(pkgs.contains("com.google.android.apps.maps"));
        assertTrue(pkgs.contains("org.videolan.vlc"));
        assertTrue(pkgs.contains("com.mitsubishielectric.ada.app.avplayer"));

        // Already fully bypassed
        assertFalse(DrivingRestrictionBypass.isBypassNeeded(xml, extra));
    }

    @Test
    public void testSanitizeHandlesNullOrMalformedXmlGracefully() {
        String fromNull = DrivingRestrictionBypass.sanitizeWhitelistXml(null, Collections.singletonList("com.app.test"));
        assertNotNull(fromNull);
        assertTrue(fromNull.contains("<package>com.app.test</package>"));
        assertTrue(fromNull.contains("<regulation>null</regulation>"));

        String fromMalformed = DrivingRestrictionBypass.sanitizeWhitelistXml("corrupted content", null);
        assertNotNull(fromMalformed);
        assertTrue(fromMalformed.contains("<whiteList>"));
        assertTrue(fromMalformed.contains("<regulation>null</regulation>"));
    }
}
