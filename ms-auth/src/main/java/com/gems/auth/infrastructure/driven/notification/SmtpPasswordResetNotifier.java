package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.PasswordResetNotifier;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

/** Sends the password reset link by e-mail through the configured SMTP server. */
public class SmtpPasswordResetNotifier implements PasswordResetNotifier {
  private final JavaMailSender mailSender;
  private final String from;
  private final String frontendUrl;

  public SmtpPasswordResetNotifier(JavaMailSender mailSender, String from, String frontendUrl) {
    this.mailSender = mailSender;
    this.from = from;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  @Override
  public Mono<Void> sendResetLink(String email, String firstName, String token) {
    String link = frontendUrl + "/auth/reset-password?token=" + token;
    return Mono.fromRunnable(() -> mailSender.send(message(email, firstName, link)))
      .subscribeOn(Schedulers.boundedElastic())
      .then();
  }

  MimeMessage message(String email, String firstName, String link) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(email);
      helper.setSubject("Restablece tu contraseña de GEMS LMS");
      String name = firstName == null || firstName.isBlank() ? "" : " " + firstName;
      helper.setText(
        "Hola" + name + ",\n\nRecibimos una solicitud para restablecer tu contraseña. Abre este enlace "
          + "(válido por 1 hora) para elegir una nueva:\n\n" + link
          + "\n\nSi no fuiste tú, ignora este correo: tu contraseña no cambia.\n",
        "<p>Hola" + HtmlUtils.htmlEscape(name) + ",</p>"
          + "<p>Recibimos una solicitud para restablecer tu contraseña. El enlace es válido por 1 hora.</p>"
          + "<p><a href=\"" + HtmlUtils.htmlEscape(link) + "\">Elegir una nueva contraseña</a></p>"
          + "<p>Si no fuiste tú, ignora este correo: tu contraseña no cambia.</p>");
      return message;
    } catch (MessagingException e) {
      throw new IllegalStateException("Could not build the password reset e-mail", e);
    }
  }
}
