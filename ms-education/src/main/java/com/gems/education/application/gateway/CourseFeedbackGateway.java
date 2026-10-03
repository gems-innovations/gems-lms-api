package com.gems.education.application.gateway;

import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import com.gems.education.domain.entities.CourseFeedback.SurveyResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourseFeedbackGateway {
  Mono<Survey> findSurvey(Long courseId);
  Mono<Survey> saveSurvey(Survey survey);

  Mono<SurveyResponse> findResponse(Long surveyId, Long studentId);
  Mono<SurveyResponse> saveResponse(SurveyResponse response);
  Flux<SurveyResponse> findResponses(Long courseId);

  Mono<Review> findReview(Long courseId, Long studentId);
  /** Saves the review and refreshes the course's average rating and rating count. */
  Mono<Review> saveReview(Review review);
  Flux<Review> findReviews(Long courseId);
}
