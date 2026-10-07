package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.EmailPreferencesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Account e-mails (created, password changed, activated). Sent in the background: the request that
 * caused them never waits for the mail server nor fails because of it.
 */
@Component
public class AccountEmails {
  private static final Logger log = LoggerFactory.getLogger(AccountEmails.class);

  private final EmailPreferencesUseCase emails;

  public AccountEmails(EmailPreferencesUseCase emails) {
    this.emails = emails;
  }

  /** An administrator created the account (one by one or from a CSV). */
  public void created(Long userId) {
    fire(emails.sendAccountNotice(userId, "Ya tienes cuenta en GEMS",
      "Te crearon una cuenta en GEMS para que estudies o enseñes en tus cursos. Entra con este correo y la "
        + "contraseña temporal que te dio tu institución; al entrar te pediremos cambiarla. Si no la tienes, usa "
        + "«¿Olvidaste tu contraseña?» en la pantalla de inicio.",
      "/auth/signin", "Entrar a GEMS"), "created");
  }

  public void passwordChanged(Long userId) {
    fire(emails.sendAccountNotice(userId, "Tu contraseña de GEMS cambió",
      "La contraseña de tu cuenta se cambió hace un momento. Si fuiste tú, no tienes que hacer nada. Si no fuiste "
        + "tú, restablécela de inmediato y avísale a tu institución.",
      "/auth/forgot-password", "No fui yo: restablecer"), "password changed");
  }

  public void activated(Long userId) {
    fire(emails.sendAccountNotice(userId, "Tu cuenta de GEMS está activa",
      "Tu institución activó tu cuenta. Ya puedes entrar y seguir con tus cursos.",
      "/auth/signin", "Entrar a GEMS"), "activated");
  }

  private static void fire(Mono<Void> send, String what) {
    send.subscribe(done -> { }, error -> log.warn("Account e-mail ({}) failed: {}", what, error.getMessage()));
  }
}
