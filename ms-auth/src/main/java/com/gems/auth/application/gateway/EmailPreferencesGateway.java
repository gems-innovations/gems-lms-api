package com.gems.auth.application.gateway;

import com.gems.auth.application.EmailPreferences;
import reactor.core.publisher.Mono;

/** Per-user e-mail choices. A user without a stored row gets {@link EmailPreferences#DEFAULTS}. */
public interface EmailPreferencesGateway {
  Mono<EmailPreferences> find(Long userId);

  Mono<Void> save(Long userId, EmailPreferences preferences);
}
