package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

/** Delivers a short notice about one of the user's courses, with a link back to the app. */
public interface CourseNoticeNotifier {
  /**
   * @param linkPath     path inside the front end (starts with "/")
   * @param preferencesToken signed token for the "manage or stop these e-mails" link
   */
  Mono<Void> sendNotice(String email, String firstName, String subject, String message, String linkPath,
                        String linkLabel, String preferencesToken);
}
