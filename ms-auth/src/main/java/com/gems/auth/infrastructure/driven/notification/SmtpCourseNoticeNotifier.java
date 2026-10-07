package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.CourseNoticeNotifier;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Sends course notices through SMTP, each with a link to change or stop these e-mails. */
public class SmtpCourseNoticeNotifier implements CourseNoticeNotifier {
  private final JavaMailSender mailSender;
  private final String from;
  private final String frontendUrl;

  public SmtpCourseNoticeNotifier(JavaMailSender mailSender, String from, String frontendUrl) {
    this.mailSender = mailSender;
    this.from = from;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  @Override
  public Mono<Void> sendNotice(String email, String firstName, String subject, String message, String linkPath,
                               String linkLabel, String preferencesToken) {
    return Mono.fromRunnable(() -> mailSender.send(message(email, firstName, subject, message, linkPath, linkLabel,
        preferencesToken)))
      .subscribeOn(Schedulers.boundedElastic())
      .then();
  }

  MimeMessage message(String email, String firstName, String subject, String text, String linkPath, String linkLabel,
                      String preferencesToken) {
    try {
      String link = frontendUrl + linkPath;
      String preferencesUrl = frontendUrl + "/correo/preferencias?t="
        + URLEncoder.encode(preferencesToken, StandardCharsets.UTF_8);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(email);
      helper.setSubject(subject);
      List<String> paragraphs = List.of(text);
      String footer = "Recibes este aviso porque estudias o enseñas en un curso de GEMS.";
      helper.setText(
        BrandedEmailTemplate.text(firstName, paragraphs, link, footer + "\nElegir qué correos recibo: " + preferencesUrl),
        BrandedEmailTemplate.html(firstName, paragraphs, linkLabel == null ? "Abrir en GEMS" : linkLabel, link, footer,
          preferencesUrl));
      // Lets mail apps show their own "unsubscribe" button.
      message.setHeader("List-Unsubscribe", "<" + preferencesUrl + ">");
      return message;
    } catch (MessagingException e) {
      throw new IllegalStateException("Could not build the course notice e-mail", e);
    }
  }
}
