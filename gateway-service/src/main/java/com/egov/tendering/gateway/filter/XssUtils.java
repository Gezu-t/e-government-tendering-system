package com.egov.tendering.gateway.filter;

import java.util.regex.Pattern;

/**
 * Utility methods for stripping XSS payloads from string values.
 *
 * <p>Targets the most dangerous vectors only — script elements, inline event handlers,
 * and pseudo-protocol URLs — without stripping arbitrary HTML that may be legitimate
 * in rich-text fields.
 */
public final class XssUtils {

    // <script ...>...</script> blocks (case-insensitive, multi-line)
    private static final Pattern SCRIPT_BLOCK = Pattern.compile(
            "<script[^>]*>[\\s\\S]*?</script>",
            Pattern.CASE_INSENSITIVE);

    // Standalone <script ...> open tags without closing (e.g. <script src="...">)
    private static final Pattern SCRIPT_TAG = Pattern.compile(
            "<script[^>]*>",
            Pattern.CASE_INSENSITIVE);

    // Inline event handlers: onXxx="..." or onXxx='...' or onXxx=...
    private static final Pattern EVENT_HANDLER = Pattern.compile(
            "\\bon\\w+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]*)",
            Pattern.CASE_INSENSITIVE);

    // javascript: and vbscript: pseudo-protocol in attribute values
    private static final Pattern JAVASCRIPT_PROTO = Pattern.compile(
            "javascript\\s*:",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VBSCRIPT_PROTO = Pattern.compile(
            "vbscript\\s*:",
            Pattern.CASE_INSENSITIVE);

    // Expression() — CSS expression injection
    private static final Pattern CSS_EXPRESSION = Pattern.compile(
            "expression\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private XssUtils() {
    }

    /**
     * Returns a sanitized copy of {@code value} with known XSS vectors removed.
     * Returns the original string unchanged if no dangerous patterns are found.
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        value = SCRIPT_BLOCK.matcher(value).replaceAll("");
        value = SCRIPT_TAG.matcher(value).replaceAll("");
        value = EVENT_HANDLER.matcher(value).replaceAll("");
        value = JAVASCRIPT_PROTO.matcher(value).replaceAll("");
        value = VBSCRIPT_PROTO.matcher(value).replaceAll("");
        value = CSS_EXPRESSION.matcher(value).replaceAll("");
        return value;
    }
}
