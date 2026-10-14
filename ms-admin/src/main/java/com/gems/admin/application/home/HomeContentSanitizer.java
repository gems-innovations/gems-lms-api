package com.gems.admin.application.home;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Limpia el HTML y el CSS que cada institución escribe para su home. No se permite JavaScript:
 * ni etiquetas script, ni manejadores on*, ni URLs javascript:, ni estilos en línea.
 */
public final class HomeContentSanitizer {
  public static final int MAX_HTML = 200_000;
  public static final int MAX_CSS = 100_000;

  private static final Safelist HTML = Safelist.relaxed()
    .addTags("section", "header", "footer", "nav", "main", "article", "aside", "figure", "figcaption",
      "details", "summary", "address", "mark", "time", "small", "hgroup")
    .addAttributes(":all", "class", "id", "title", "lang", "role", "aria-label", "aria-labelledby",
      "aria-describedby", "aria-hidden")
    .addAttributes("img", "loading")
    .addProtocols("img", "src", "data")
    .addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow")
    .preserveRelativeLinks(true);

  private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
  private static final Pattern CSS_IMPORT = Pattern.compile("@import[^;{}]*;?", Pattern.CASE_INSENSITIVE);
  private static final Pattern CSS_DANGEROUS = Pattern.compile(
    "expression\\s*\\(|behavior\\s*:|-moz-binding\\s*:|javascript\\s*:|vbscript\\s*:", Pattern.CASE_INSENSITIVE);
  private static final Pattern CSS_URL = Pattern.compile(
    "url\\(\\s*(?:\"([^\"]*)\"|'([^']*)'|([^)\"'\\s]*))\\s*\\)", Pattern.CASE_INSENSITIVE);

  private HomeContentSanitizer() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String cleanHtml(String html) {
    if (html == null || html.isBlank()) {
      return "";
    }
    return Jsoup.clean(html, "https://home.invalid/", HTML, new Document.OutputSettings().prettyPrint(false));
  }

  /** Quita @import, escapes, construcciones peligrosas y url() que no sean https, relativas o data:image. */
  public static String cleanCss(String css) {
    if (css == null || css.isBlank()) {
      return "";
    }
    String clean = CSS_COMMENT.matcher(css).replaceAll("").replace("\\", "").replace("<", "");
    clean = CSS_IMPORT.matcher(clean).replaceAll("");
    clean = CSS_DANGEROUS.matcher(clean).replaceAll("");
    Matcher matcher = CSS_URL.matcher(clean);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String target = firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3)).trim();
      matcher.appendReplacement(out, Matcher.quoteReplacement(allowedUrl(target) ? matcher.group() : "none"));
    }
    matcher.appendTail(out);
    return out.toString().trim();
  }

  private static boolean allowedUrl(String target) {
    String lower = target.toLowerCase(Locale.ROOT);
    return lower.startsWith("https://") || lower.startsWith("/") || lower.startsWith("#")
      || lower.startsWith("data:image/");
  }

  private static String firstNonNull(String... values) {
    for (String value : values) {
      if (value != null) {
        return value;
      }
    }
    return "";
  }
}
