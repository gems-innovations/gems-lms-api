package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

/** Keeps proof of every authorization a user gives or withdraws. Append only. */
public interface ConsentGateway {
  /** The data processing policy, accepted when the account is created. */
  String DATA_POLICY = "data_policy";
  /** Reminders and ideas by e-mail (promotional): opt-in. */
  String TIPS = "tips";

  Mono<Void> record(Long userId, String kind, String policyVersion, boolean granted);
}
