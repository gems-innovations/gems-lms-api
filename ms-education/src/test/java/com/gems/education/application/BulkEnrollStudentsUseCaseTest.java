package com.gems.education.application;

import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Course;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkEnrollStudentsUseCaseTest {

  @Mock
  private EnrollmentGateway enrollmentGateway;

  @Mock
  private StudentGateway studentGateway;

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase;

  @Test
  void shouldBulkEnrollStudentsSuccessfully() {
    BulkEnrollmentCommand command = new BulkEnrollmentCommand(List.of(10L), 5L);
    Student student = new Student(10L, "Juan", "juan@gmail.com", LocalDate.of(2000, 1, 1), "Colombia", "Medellin", "CC", "123456");
    Course course = new Course(5L, "Java", "Desc", "PUBLISHED", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of());
    Enrollment enrollment = new Enrollment(1L, 10L, 5L, LocalDateTime.now(), 0, null);

    when(courseGateway.findById(5L)).thenReturn(Mono.just(course));
    when(studentGateway.findById(any(StudentId.class))).thenReturn(Mono.just(student));
    when(enrollmentGateway.findByStudentIdAndCourseId(10L, 5L)).thenReturn(Mono.empty());
    when(enrollmentGateway.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

    Flux<EnrollmentResponse> result = bulkEnrollStudentsUseCase.execute(command);

    StepVerifier.create(result)
      .expectNextMatches(res -> res.getStudentId().equals(10L) && res.getCourseId().equals(5L))
      .verifyComplete();

    verify(courseGateway, times(1)).findById(5L);
    verify(studentGateway, times(1)).findById(any(StudentId.class));
    verify(enrollmentGateway, times(1)).save(any(Enrollment.class));
  }
}
