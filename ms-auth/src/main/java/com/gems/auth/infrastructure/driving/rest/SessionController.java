package com.gems.auth.infrastructure.driving.rest;

import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.SessionValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SessionController {
  private final SessionValidator sessions;

  public SessionController(SessionValidator sessions) {
    this.sessions = sessions;
  }

  @GetMapping("/api/v1/auth/session")
  public Mono<SessionValidator.State> session() {
    return CurrentUser.forPasswordChange().flatMap(user -> sessions.state(user.userId(), ""));
  }
}
