package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.StudentCommand;
import com.gems.education.infrastructure.driving.rest.request.StudentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentMapper Tests")
class StudentMapperTest {

  @Nested
  @DisplayName("Utility Class Structure Tests")
  class UtilityClassTests {

    @Test
    @DisplayName("Should not allow instantiation")
    void shouldNotAllowInstantiation() throws Exception {
      Constructor<StudentMapper> constructor =
        StudentMapper.class.getDeclaredConstructor();

      assertTrue(Modifier.isPrivate(constructor.getModifiers()));

      constructor.setAccessible(true);

      Exception exception =
        assertThrows(Exception.class, constructor::newInstance);

      assertTrue(exception.getCause() instanceof UnsupportedOperationException);
      assertEquals("Utility class", exception.getCause().getMessage());
    }
  }

  @Nested
  @DisplayName("toDomain() Mapping Tests")
  class ToDomainTests {

    @Test
    @DisplayName("Should correctly map StudentRequest to StudentCommand")
    void shouldMapRequestToDomain() {
      StudentRequest request = new StudentRequest();
      request.setName("Juan");
      request.setEmail("juan@example.com");
      request.setBirthDate(LocalDate.of(2000, 1, 1));
      request.setCountry("Colombia");
      request.setCity("Medellín");
      request.setDocumentType("CC");
      request.setDocumentNumber("123456");

      StudentCommand command = StudentMapper.toDomain(request);

      assertNotNull(command);
      assertEquals("Juan", command.getName());
      assertEquals("juan@example.com", command.getEmail());
      assertEquals(LocalDate.of(2000, 1, 1), command.getBirthDate());
      assertEquals("Colombia", command.getCountry());
      assertEquals("Medellín", command.getCity());
      assertEquals("CC", command.getDocumentType());
      assertEquals("123456", command.getDocumentNumber());
    }

    @Test
    @DisplayName("Should throw NullPointerException if request is null")
    void shouldThrowWhenRequestIsNull() {
      assertThrows(NullPointerException.class, () -> StudentMapper.toDomain(null));
    }
  }
}