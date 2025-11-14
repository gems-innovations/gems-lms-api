package com.gems.education.infrastructure.config;

import com.gems.education.application.DeleteStudentUseCase;
import com.gems.education.application.GetAllStudentsUseCase;
import com.gems.education.application.GetStudentByIdUseCase;
import com.gems.education.application.RegisterStudentUseCase;
import com.gems.education.application.UpdateStudentUseCase;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.infrastructure.driven.postgresql.IStudentRepository;
import com.gems.education.infrastructure.driven.postgresql.StudentRepositoryAdapter;
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
}
