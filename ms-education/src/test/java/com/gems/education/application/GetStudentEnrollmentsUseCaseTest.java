package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.StudentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetStudentEnrollmentsUseCaseTest {

  @Mock
  private EnrollmentGateway enrollmentGateway;

  @Mock
  private StudentGateway studentGateway;

  @InjectMocks
  private GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase;

  @Test
  void shouldReturnStudentEnrollments() {
    Student student = new Student(10L, "Juan", "juan@gmail.com", LocalDate.of(2000, 1, 1), "Colombia", "Medellin", "CC", "123456");
    Enrollment enrollment = new Enrollment(1L, 10L, 5L, LocalDateTime.now(), 20, null);

    when(studentGateway.findById(any(StudentId.class))).thenReturn(Mono.just(student));
    when(enrollmentGateway.findByStudentId(10L)).thenReturn(Flux.just(enrollment));

    Flux<EnrollmentResponse> result = getStudentEnrollmentsUseCase.execute(10L);

    StepVerifier.create(result)
      .expectNextMatches(res -> res.getStudentId().equals(10L) && res.getProgress() == 20)
      .verifyComplete();

    verify(studentGateway, times(1)).findById(any(StudentId.class));
    verify(enrollmentGateway, times(1)).findByStudentId(10L);
  }
}
