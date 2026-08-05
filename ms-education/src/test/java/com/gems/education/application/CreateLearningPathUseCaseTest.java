package com.gems.education.application;

import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateLearningPathUseCaseTest {

  @Mock
  private LearningPathGateway learningPathGateway;

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private CreateLearningPathUseCase createLearningPathUseCase;

  @Test
  void shouldCreateLearningPathSuccessfully() {
    LearningPathCommand command = new LearningPathCommand("Java Track", "Desc", "inst-1", List.of(1L));
    Course course = new Course(1L, "Java 1", "Desc", "PUBLISHED", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of());
    LearningPath lp = new LearningPath(1L, "Java Track", "Desc", "inst-1", LocalDateTime.now(), List.of(course));

    when(courseGateway.findById(1L)).thenReturn(Mono.just(course));
    when(learningPathGateway.save(any(LearningPath.class))).thenReturn(Mono.just(lp));

    Mono<LearningPathResponse> result = createLearningPathUseCase.execute(command);

    StepVerifier.create(result)
      .expectNextMatches(res ->
        res.getId().equals(1L) &&
          res.getTitle().equals("Java Track") &&
          res.getCourses().size() == 1
      )
      .verifyComplete();

    verify(courseGateway, times(1)).findById(1L);
    verify(learningPathGateway, times(1)).save(any(LearningPath.class));
  }
}
