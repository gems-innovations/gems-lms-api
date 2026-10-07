package com.gems.auth.infrastructure.driven.notification;

import org.springframework.web.util.HtmlUtils;

/** Builds the HTML shell shared by every GEMS e-mail: header, one call to action and a legal footer. */
public final class BrandedEmailTemplate {
  private BrandedEmailTemplate() {
  }

  /**
   * @param greetingName first name, may be blank
   * @param paragraphs   plain-text paragraphs (escaped here)
   * @param buttonLabel  label of the call to action, or null for none
   * @param buttonUrl    destination of the call to action
   * @param footerNote   why the user is receiving this message (plain text, escaped here)
   */
  public static String html(String greetingName, java.util.List<String> paragraphs, String buttonLabel,
                            String buttonUrl, String footerNote) {
    return html(greetingName, paragraphs, buttonLabel, buttonUrl, footerNote, null);
  }

  /** Same as above, plus a "manage these e-mails" link in the footer when {@code preferencesUrl} is set. */
  public static String html(String greetingName, java.util.List<String> paragraphs, String buttonLabel,
                            String buttonUrl, String footerNote, String preferencesUrl) {
    String preferences = preferencesUrl == null ? ""
      : "<br><a href=\"" + HtmlUtils.htmlEscape(preferencesUrl) + "\" style=\"color:#4f46e5\">"
        + "Elegir qué correos recibo o dejar de recibirlos</a>";
    StringBuilder body = new StringBuilder();
    body.append("<p style=\"margin:0 0 16px\">Hola").append(name(greetingName)).append(",</p>");
    for (String paragraph : paragraphs) {
      body.append("<p style=\"margin:0 0 16px\">").append(HtmlUtils.htmlEscape(paragraph)).append("</p>");
    }
    if (buttonLabel != null && buttonUrl != null) {
      body.append("<p style=\"margin:24px 0\"><a href=\"").append(HtmlUtils.htmlEscape(buttonUrl))
        .append("\" style=\"background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 24px;")
        .append("border-radius:8px;font-weight:600;display:inline-block\">")
        .append(HtmlUtils.htmlEscape(buttonLabel)).append("</a></p>")
        .append("<p style=\"margin:0 0 16px;font-size:13px;color:#6b7280\">Si el botón no funciona, copia este enlace: ")
        .append("<br><span style=\"word-break:break-all\">").append(HtmlUtils.htmlEscape(buttonUrl)).append("</span></p>");
    }
    return "<!doctype html><html lang=\"es\"><body style=\"margin:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif\">"
      + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">"
      + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%;background:#ffffff;border-radius:12px\">"
      + "<tr><td style=\"padding:20px 32px;border-bottom:1px solid #e5e7eb;font-size:20px;font-weight:700;color:#4f46e5\">GEMS</td></tr>"
      + "<tr><td style=\"padding:28px 32px;font-size:16px;line-height:1.5;color:#111827\">" + body + "</td></tr>"
      + "<tr><td style=\"padding:16px 32px;border-top:1px solid #e5e7eb;font-size:12px;line-height:1.5;color:#6b7280\">"
      + HtmlUtils.htmlEscape(footerNote) + preferences + "<br>GEMS Innovations · info@gemsinnovations.com</td></tr>"
      + "</table></td></tr></table></body></html>";
  }

  /** Plain-text alternative so clients that block HTML still get the message. */
  public static String text(String greetingName, java.util.List<String> paragraphs, String buttonUrl, String footerNote) {
    StringBuilder text = new StringBuilder("Hola").append(name(greetingName)).append(",\n\n");
    for (String paragraph : paragraphs) {
      text.append(paragraph).append("\n\n");
    }
    if (buttonUrl != null) {
      text.append(buttonUrl).append("\n\n");
    }
    return text.append(footerNote).append("\n— GEMS Innovations").toString();
  }

  private static String name(String firstName) {
    return firstName == null || firstName.isBlank() ? "" : " " + HtmlUtils.htmlEscape(firstName);
  }
}
