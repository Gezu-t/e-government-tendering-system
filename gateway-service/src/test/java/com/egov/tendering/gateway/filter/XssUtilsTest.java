package com.egov.tendering.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XssUtilsTest {

    @Test
    void nullAndEmptyPassThrough() {
        assertNull(XssUtils.sanitize(null));
        assertEquals("", XssUtils.sanitize(""));
    }

    @Test
    void cleanInputIsUnchanged() {
        String clean = "{\"name\":\"John\",\"amount\":100}";
        assertEquals(clean, XssUtils.sanitize(clean));
    }

    @Test
    void stripsScriptBlocks() {
        assertEquals("before  after",
                XssUtils.sanitize("before <script>alert('xss')</script> after"));
    }

    @Test
    void stripsScriptBlocksCaseInsensitive() {
        assertEquals("x",
                XssUtils.sanitize("x<SCRIPT type=\"text/javascript\">doEvil()</SCRIPT>"));
    }

    @Test
    void stripsStandaloneScriptTags() {
        assertEquals("hello",
                XssUtils.sanitize("<script src=\"evil.js\">hello"));
    }

    @Test
    void stripsEventHandlers() {
        assertEquals("<img src=\"pic.jpg\" >",
                XssUtils.sanitize("<img src=\"pic.jpg\" onerror=\"alert(1)\">"));
    }

    @Test
    void stripsJavascriptProtocol() {
        assertEquals("<a href=\"alert(1)\">click</a>",
                XssUtils.sanitize("<a href=\"javascript:alert(1)\">click</a>"));
    }

    @Test
    void stripsVbscriptProtocol() {
        assertEquals("MsgBox",
                XssUtils.sanitize("vbscript:MsgBox"));
    }

    @Test
    void stripsCssExpression() {
        assertEquals("width: alert(1))100px",
                XssUtils.sanitize("width: expression(alert(1))100px"));
    }
}
