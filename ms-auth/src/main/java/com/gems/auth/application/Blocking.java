package com.gems.auth.application;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/**
 * Runs CPU-heavy or blocking work (BCrypt takes around 100 ms) off the event-loop threads, which
 * serve every request of the service: a burst of sign-ins must not freeze the rest of the API.
 */
final class Blocking {

  private Blocking() {
  }

  static <T> Mono<T> offload(Callable<T> work) {
    return Mono.fromCallable(work).subscribeOn(Schedulers.boundedElastic());
  }
}
