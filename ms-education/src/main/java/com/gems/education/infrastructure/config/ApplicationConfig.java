package com.gems.education.infrastructure.config;

import com.gems.education.application.RegisterStudentUseCase;
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
}
