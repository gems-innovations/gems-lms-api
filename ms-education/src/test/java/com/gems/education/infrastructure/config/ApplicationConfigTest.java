package com.gems.education.infrastructure.config;

import com.gems.education.application.*;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.infrastructure.driven.postgresql.IStudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ApplicationConfig Tests (Education Module)")
class ApplicationConfigTest {

  @Test
  @DisplayName("Should create StudentGateway bean")
  void shouldCreateStudentGatewayBean() {
    ApplicationConfig config = new ApplicationConfig();

    IStudentRepository studentRepository = mock(IStudentRepository.class);

    StudentGateway gateway = config.studentGateway(studentRepository);

    assertNotNull(gateway);
  }

  @Test
  @DisplayName("Should create RegisterStudentUseCase bean")
  void shouldCreateRegisterStudentUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    StudentGateway gateway = mock(StudentGateway.class);

    RegisterStudentUseCase useCase = config.registerStudentUseCase(gateway);

    assertNotNull(useCase);
  }

  @Test
  @DisplayName("Should create GetStudentByIdUseCase bean")
  void shouldCreateGetStudentByIdUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    StudentGateway gateway = mock(StudentGateway.class);

    GetStudentByIdUseCase useCase = config.getStudentByIdUseCase(gateway);

    assertNotNull(useCase);
  }

  @Test
  @DisplayName("Should create GetAllStudentsUseCase bean")
  void shouldCreateGetAllStudentsUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    StudentGateway gateway = mock(StudentGateway.class);

    GetAllStudentsUseCase useCase = config.getAllStudentsUseCase(gateway);

    assertNotNull(useCase);
  }

  @Test
  @DisplayName("Should create UpdateStudentUseCase bean")
  void shouldCreateUpdateStudentUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    StudentGateway gateway = mock(StudentGateway.class);

    UpdateStudentUseCase useCase = config.updateStudentUseCase(gateway);

    assertNotNull(useCase);
  }

  @Test
  @DisplayName("Should create DeleteStudentUseCase bean")
  void shouldCreateDeleteStudentUseCaseBean() {
    ApplicationConfig config = new ApplicationConfig();
    StudentGateway gateway = mock(StudentGateway.class);

    DeleteStudentUseCase useCase = config.deleteStudentUseCase(gateway);

    assertNotNull(useCase);
  }
}