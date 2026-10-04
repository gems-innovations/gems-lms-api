package com.gems.auth.infrastructure.driven.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationConfigTest {
  private final NotificationConfig config = new NotificationConfig();

  @Test
  void withoutMailHostUsesSafeUndeliveredFallback() {
    assertInstanceOf(UndeliveredPasswordResetNotifier.class,
      config.passwordResetNotifier("http://localhost:4200", "", 587, "", "", true, "no-reply@gems.lms"));
  }

  @Test
  void withMailHostLinksAreEmailed() {
    assertInstanceOf(SmtpPasswordResetNotifier.class,
      config.passwordResetNotifier("http://localhost:4200", "smtp.example.com", 587, "user", "pass", true, "no-reply@gems.lms"));
  }

  @Test
  void theEmailCarriesTheResetLink() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    SmtpPasswordResetNotifier notifier = new SmtpPasswordResetNotifier(sender, "no-reply@gems.lms", "https://lms.example.com/");

    MimeMessage message = notifier.message("ana@example.com", "Ana", "https://lms.example.com/auth/reset-password?token=abc");
    message.saveChanges();

    assertEquals("ana@example.com", message.getAllRecipients()[0].toString());
    assertTrue(message.getSubject().contains("contraseña"));
    java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
    message.writeTo(raw);
    assertTrue(raw.toString(java.nio.charset.StandardCharsets.UTF_8).contains("reset-password?token"));
  }
}
