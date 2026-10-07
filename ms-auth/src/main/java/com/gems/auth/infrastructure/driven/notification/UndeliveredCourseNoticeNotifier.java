package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.CourseNoticeNotifier;
import reactor.core.publisher.Mono;

/** Used when no SMTP server is configured: notices stay in the app only. */
public class UndeliveredCourseNoticeNotifier implements CourseNoticeNotifier {
  @Override
  public Mono<Void> sendNotice(String email, String firstName, String subject, String message, String linkPath,
                               String linkLabel, String preferencesToken) {
    return Mono.empty();
  }
}
