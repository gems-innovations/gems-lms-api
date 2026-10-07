package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

import java.util.List;

/** Asks ms-auth to e-mail a course notice; ms-auth applies each user's preferences and skips guests. */
public interface EmailNoticeGateway {
  /** Never fails: a notice that cannot be e-mailed is still shown in the app. */
  Mono<Void> send(List<Long> userIds, String subject, String message, String linkPath, String linkLabel);

  /** To the teachers and administrators of the institution. */
  default Mono<Void> sendToStaff(String institutionId, String subject, String message, String linkPath,
                                 String linkLabel) {
    return Mono.empty();
  }

  /** To an address that is not a user, or to every super admin when {@code to} is {@link #SUPER_ADMINS}. */
  default Mono<Void> sendToAddress(String to, String name, String subject, String message, String linkPath,
                                   String linkLabel) {
    return Mono.empty();
  }

  /** Motivation e-mail (streak, "we miss you"); ms-auth only sends it to students who keep tips on. */
  default Mono<Void> sendTip(Long userId, String subject, String message, String linkPath, String linkLabel) {
    return Mono.empty();
  }

  String SUPER_ADMINS = "super-admins";

  /** For tests and setups without e-mail. */
  EmailNoticeGateway NONE = (userIds, subject, message, linkPath, linkLabel) -> Mono.empty();
}
