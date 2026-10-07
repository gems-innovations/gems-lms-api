package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.EmailVerificationNotifier;
import com.gems.auth.application.gateway.CourseNoticeNotifier;
import com.gems.auth.application.gateway.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Picks how e-mails are delivered. Tokens are never written to logs.
 */
@Configuration
public class NotificationConfig {
  private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

  @Bean
  public PasswordResetNotifier passwordResetNotifier(
      @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
      @Value("${app.mail.host:}") String host,
      @Value("${app.mail.port:587}") int port,
      @Value("${app.mail.username:}") String username,
      @Value("${app.mail.password:}") String password,
      @Value("${app.mail.starttls:true}") boolean starttls,
      @Value("${app.mail.from:no-reply@gems.lms}") String from) {
    if (host.isBlank()) {
      log.warn("MAIL_HOST is not set: password reset messages will not be delivered");
      return new UndeliveredPasswordResetNotifier();
    }
    return new SmtpPasswordResetNotifier(sender(host, port, username, password, starttls), from, frontendUrl);
  }

  @Bean
  public EmailVerificationNotifier emailVerificationNotifier(
      @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
      @Value("${app.mail.host:}") String host,
      @Value("${app.mail.port:587}") int port,
      @Value("${app.mail.username:}") String username,
      @Value("${app.mail.password:}") String password,
      @Value("${app.mail.starttls:true}") boolean starttls,
      @Value("${app.mail.from:no-reply@gems.lms}") String from) {
    if (host.isBlank()) {
      log.warn("MAIL_HOST is not set: e-mail verification messages will not be delivered");
      return new UndeliveredEmailVerificationNotifier();
    }
    return new SmtpEmailVerificationNotifier(sender(host, port, username, password, starttls), from, frontendUrl);
  }

  @Bean
  public CourseNoticeNotifier courseNoticeNotifier(
      @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
      @Value("${app.mail.host:}") String host,
      @Value("${app.mail.port:587}") int port,
      @Value("${app.mail.username:}") String username,
      @Value("${app.mail.password:}") String password,
      @Value("${app.mail.starttls:true}") boolean starttls,
      @Value("${app.mail.from:no-reply@gems.lms}") String from) {
    if (host.isBlank()) {
      log.warn("MAIL_HOST is not set: course notices will stay in the app only");
      return new UndeliveredCourseNoticeNotifier();
    }
    return new SmtpCourseNoticeNotifier(sender(host, port, username, password, starttls), from, frontendUrl);
  }

  private static JavaMailSenderImpl sender(String host, int port, String username, String password, boolean starttls) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    if (!username.isBlank()) {
      sender.setUsername(username);
      sender.setPassword(password);
    }
    Properties props = sender.getJavaMailProperties();
    props.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
    props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
    props.put("mail.smtp.connectiontimeout", "10000");
    props.put("mail.smtp.timeout", "10000");
    props.put("mail.smtp.writetimeout", "10000");
    return sender;
  }
}
