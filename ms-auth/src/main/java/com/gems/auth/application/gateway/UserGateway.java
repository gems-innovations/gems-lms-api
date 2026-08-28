package com.gems.auth.application.gateway;

import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserGateway {
  Mono<User> save(User user);
  Mono<User> findById(UserId id);
  Mono<User> findByEmail(Email email);
  Mono<Boolean> existsByEmail(Email email);
  Mono<Void> deleteById(UserId id);
  Flux<User> findByInstitutionId(String institutionId);
  Flux<User> findAll();
}
