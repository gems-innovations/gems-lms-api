package com.gems.education.infrastructure.driving.rest.request;

import com.gems.education.infrastructure.driving.rest.constants.RestConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StudentRequestTest {

  private Validator validator;

  @BeforeEach
  void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  private StudentRequest validRequest() {
    return new StudentRequest(
      "Juan Pérez",
      "juan@example.com",
      LocalDate.of(2000, 1, 1),
      "Colombia",
      "Medellín",
      "CC",
      "123456789"
    );
  }

  @Test
  void validRequestShouldHaveNoViolations() {
    Set<ConstraintViolation<StudentRequest>> violations = validator.validate(validRequest());
    assertTrue(violations.isEmpty());
  }

  @Nested
  class NameValidation {
    @Test
    void blankNameShouldFail() {
      StudentRequest r = validRequest();
      r.setName("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.NAME_REQUIRED_MESSAGE)));
    }

    @Test
    void shortNameShouldFail() {
      StudentRequest r = validRequest();
      r.setName("J");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.NAME_SIZE_MESSAGE)));
    }

    @Test
    void longNameShouldFail() {
      StudentRequest r = validRequest();
      r.setName("A".repeat(51));
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.NAME_SIZE_MESSAGE)));
    }
  }

  @Nested
  class EmailValidation {
    @Test
    void blankEmailShouldFail() {
      StudentRequest r = validRequest();
      r.setEmail("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.EMAIL_REQUIRED_MESSAGE)));
    }

    @Test
    void invalidEmailShouldFail() {
      StudentRequest r = validRequest();
      r.setEmail("invalid");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.EMAIL_VALID_MESSAGE)));
    }
  }

  @Nested
  class BirthDateValidation {
    @Test
    void futureBirthDateShouldFail() {
      StudentRequest r = validRequest();
      r.setBirthDate(LocalDate.now().plusDays(1));
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.BIRTHDATE_FUTURE_MESSAGE)));
    }
  }

  @Nested
  class CountryValidation {
    @Test
    void blankCountryShouldFail() {
      StudentRequest r = validRequest();
      r.setCountry("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.COUNTRY_REQUIRED_MESSAGE)));
    }
  }

  @Nested
  class CityValidation {
    @Test
    void blankCityShouldFail() {
      StudentRequest r = validRequest();
      r.setCity("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.CITY_REQUIRED_MESSAGE)));
    }
  }

  @Nested
  class DocumentTypeValidation {
    @Test
    void blankDocumentTypeShouldFail() {
      StudentRequest r = validRequest();
      r.setDocumentType("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.DOCUMENT_TYPE_REQUIRED_MESSAGE)));
    }
  }

  @Nested
  class DocumentNumberValidation {
    @Test
    void blankDocumentNumberShouldFail() {
      StudentRequest r = validRequest();
      r.setDocumentNumber("");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.DOCUMENT_NUMBER_REQUIRED_MESSAGE)));
    }

    @Test
    void shortDocumentNumberShouldFail() {
      StudentRequest r = validRequest();
      r.setDocumentNumber("1234");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.DOCUMENT_NUMBER_SIZE_MESSAGE)));
    }

    @Test
    void longDocumentNumberShouldFail() {
      StudentRequest r = validRequest();
      r.setDocumentNumber("1".repeat(21));
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.DOCUMENT_NUMBER_SIZE_MESSAGE)));
    }

    @Test
    void invalidPatternShouldFail() {
      StudentRequest r = validRequest();
      r.setDocumentNumber("ABC###");
      Set<ConstraintViolation<StudentRequest>> v = validator.validate(r);
      assertTrue(v.stream().anyMatch(e -> e.getMessage().equals(RestConstants.DOCUMENT_NUMBER_PATTERN_MESSAGE)));
    }
  }
}