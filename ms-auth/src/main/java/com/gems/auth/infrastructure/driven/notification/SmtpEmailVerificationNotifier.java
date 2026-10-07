package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.EmailVerificationNotifier;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Sends the e-mail verification link through the configured SMTP server. */
public class SmtpEmailVerificationNotifier implements EmailVerificationNotifier {
  private final JavaMailSender mailSender;
  private final String from;
  private final String frontendUrl;

  public SmtpEmailVerificationNotifier(JavaMailSender mailSender, String from, String frontendUrl) {
    this.mailSender = mailSender;
    this.from = from;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  @Override
  public Mono<Void> sendVerification(String email, String firstName, String token) {
    String link = frontendUrl + "/auth/verify-email?token=" + token;
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
      helper.setSubject("Confirma tu correo en GEMS");
      List<String> paragraphs = List.of(
        "¡Bienvenido a GEMS! Confirma que este correo es tuyo para proteger tu cuenta y poder recuperarla si olvidas la contraseña.",
        "El enlace es válido por 24 horas.");
      String footer = "Recibes este mensaje porque se creó una cuenta GEMS con este correo. Si no fuiste tú, ignóralo.";
      helper.setText(BrandedEmailTemplate.text(firstName, paragraphs, link, footer),
        BrandedEmailTemplate.html(firstName, paragraphs, "Confirmar mi correo", link, footer));
      return message;
    } catch (MessagingException e) {
      throw new IllegalStateException("Could not build the verification e-mail", e);
    }
  }
}
