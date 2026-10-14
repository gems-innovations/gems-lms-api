package com.gems.admin.application.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomeContentSanitizerTest {

  @Test
  void shouldRemoveScriptsAndEventHandlersWhenCleaningHtml() {
    String clean = HomeContentSanitizer.cleanHtml(
      "<section class=\"hero\"><h1>Hola</h1><script>alert(1)</script>"
        + "<img src=\"https://x.test/a.png\" onerror=\"alert(2)\"><a href=\"javascript:alert(3)\">x</a></section>");

    assertTrue(clean.contains("<h1>Hola</h1>"));
    assertTrue(clean.contains("class=\"hero\""));
    assertFalse(clean.contains("script"));
    assertFalse(clean.contains("onerror"));
    assertFalse(clean.contains("javascript:"));
  }

  @Test
  void shouldDropInlineStylesAndForms() {
    String clean = HomeContentSanitizer.cleanHtml("<div style=\"position:fixed\"><form><input></form>ok</div>");

    assertFalse(clean.contains("style"));
    assertFalse(clean.contains("<form"));
    assertTrue(clean.contains("ok"));
  }

  @Test
  void shouldKeepRelativeLinks() {
    String clean = HomeContentSanitizer.cleanHtml("<a href=\"/auth/login\">Entrar</a>");

    assertTrue(clean.contains("href=\"/auth/login\""));
    assertTrue(clean.contains("rel=\"noopener noreferrer nofollow\""));
  }

  @Test
  void shouldStripImportsAndDangerousConstructsFromCss() {
    String clean = HomeContentSanitizer.cleanCss(
      "@import url('https://evil.test/x.css'); .a { width: expression(alert(1)); color: red; }");

    assertFalse(clean.contains("@import"));
    assertFalse(clean.contains("expression"));
    assertTrue(clean.contains("color: red"));
  }

  @Test
  void shouldOnlyAllowSafeUrlsInCss() {
    String clean = HomeContentSanitizer.cleanCss(
      ".a { background: url(https://cdn.test/a.png); } .b { background: url('http://evil.test/a.png'); }"
        + " .c { background: url(javascript:alert(1)); }");

    assertTrue(clean.contains("url(https://cdn.test/a.png)"));
    assertFalse(clean.contains("evil.test"));
    assertFalse(clean.contains("javascript"));
  }

  @Test
  void shouldReturnEmptyWhenBlank() {
    assertEquals("", HomeContentSanitizer.cleanHtml("  "));
    assertEquals("", HomeContentSanitizer.cleanCss(null));
  }
}
