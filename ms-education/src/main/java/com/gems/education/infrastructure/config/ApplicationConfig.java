package com.gems.education.infrastructure.config;

import com.gems.education.application.*;
import com.gems.education.application.gateway.*;
import com.gems.education.infrastructure.driven.postgresql.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public StudentGateway studentGateway(IStudentRepository studentRepository) {
    return new StudentRepositoryAdapter(studentRepository);
  }

  @Bean
  public RegisterStudentUseCase registerStudentUseCase(StudentGateway studentGateway) {
    return new RegisterStudentUseCase(studentGateway);
  }

  @Bean
  public DeleteStudentUseCase deleteStudentUseCase(StudentGateway studentGateway) {
    return new DeleteStudentUseCase(studentGateway);
  }

  @Bean
  public GetAllStudentsUseCase getAllStudentsUseCase(StudentGateway studentGateway) {
    return new GetAllStudentsUseCase(studentGateway);
  }

  @Bean
  public GetStudentByIdUseCase getStudentByIdUseCase(StudentGateway studentGateway) {
    return new GetStudentByIdUseCase(studentGateway);
  }

  @Bean
  public UpdateStudentUseCase updateStudentUseCase(StudentGateway studentGateway) {
    return new UpdateStudentUseCase(studentGateway);
  }

  @Bean
  public CourseGateway courseGateway(ICourseRepository courseRepository,
                                     IModuleRepository moduleRepository,
                                     ILessonRepository lessonRepository,
                                     IContentRepository contentRepository) {
    return new CourseRepositoryAdapter(courseRepository, moduleRepository, lessonRepository, contentRepository);
  }

  @Bean
  public CreateCourseUseCase createCourseUseCase(CourseGateway courseGateway) {
    return new CreateCourseUseCase(courseGateway);
  }

  @Bean
  public GetCourseByIdUseCase getCourseByIdUseCase(CourseGateway courseGateway) {
    return new GetCourseByIdUseCase(courseGateway);
  }

  @Bean
  public UpdateCourseUseCase updateCourseUseCase(CourseGateway courseGateway) {
    return new UpdateCourseUseCase(courseGateway);
  }

  @Bean
  public DeleteCourseUseCase deleteCourseUseCase(CourseGateway courseGateway) {
    return new DeleteCourseUseCase(courseGateway);
  }

  @Bean
  public GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase(CourseGateway courseGateway) {
    return new GetCoursesByInstitutionUseCase(courseGateway);
  }

  @Bean
  public QuizGateway quizGateway(IQuizRepository quizRepository, IQuestionRepository questionRepository) {
    return new QuizRepositoryAdapter(quizRepository, questionRepository);
  }

  @Bean
  public CreateQuizUseCase createQuizUseCase(QuizGateway quizGateway) {
    return new CreateQuizUseCase(quizGateway);
  }

  @Bean
  public GetQuizByLessonUseCase getQuizByLessonUseCase(QuizGateway quizGateway) {
    return new GetQuizByLessonUseCase(quizGateway);
  }

  @Bean
  public SubmitQuizUseCase submitQuizUseCase(QuizGateway quizGateway) {
    return new SubmitQuizUseCase(quizGateway);
  }

  @Bean
  public LearningPathGateway learningPathGateway(ILearningPathRepository lpRepo, ILearningPathCourseRepository lpcRepo, CourseGateway courseGateway) {
    return new LearningPathRepositoryAdapter(lpRepo, lpcRepo, courseGateway);
  }

  @Bean
  public CreateLearningPathUseCase createLearningPathUseCase(LearningPathGateway learningPathGateway, CourseGateway courseGateway) {
    return new CreateLearningPathUseCase(learningPathGateway, courseGateway);
  }

  @Bean
  public GetLearningPathByIdUseCase getLearningPathByIdUseCase(LearningPathGateway learningPathGateway) {
    return new GetLearningPathByIdUseCase(learningPathGateway);
  }

  @Bean
  public GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase(LearningPathGateway learningPathGateway) {
    return new GetLearningPathsByInstitutionUseCase(learningPathGateway);
  }

  @Bean
  public UpdateLearningPathUseCase updateLearningPathUseCase(LearningPathGateway learningPathGateway, CourseGateway courseGateway) {
    return new UpdateLearningPathUseCase(learningPathGateway, courseGateway);
  }

  @Bean
  public DeleteLearningPathUseCase deleteLearningPathUseCase(LearningPathGateway learningPathGateway) {
    return new DeleteLearningPathUseCase(learningPathGateway);
  }

  @Bean
  public EnrollmentGateway enrollmentGateway(IEnrollmentRepository enrollmentRepository) {
    return new EnrollmentRepositoryAdapter(enrollmentRepository);
  }

  @Bean
  public EnrollStudentUseCase enrollStudentUseCase(EnrollmentGateway enrollmentGateway, StudentGateway studentGateway, CourseGateway courseGateway) {
    return new EnrollStudentUseCase(enrollmentGateway, studentGateway, courseGateway);
  }

  @Bean
  public BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase(EnrollmentGateway enrollmentGateway, StudentGateway studentGateway, CourseGateway courseGateway) {
    return new BulkEnrollStudentsUseCase(enrollmentGateway, studentGateway, courseGateway);
  }

  @Bean
  public GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase(EnrollmentGateway enrollmentGateway, StudentGateway studentGateway) {
    return new GetStudentEnrollmentsUseCase(enrollmentGateway, studentGateway);
  }

  @Bean
  public UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase(EnrollmentGateway enrollmentGateway) {
    return new UpdateEnrollmentProgressUseCase(enrollmentGateway);
  }
}
