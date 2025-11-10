package com.gems.education.infrastructure.driving.rest.request;

import java.time.LocalDate;

import com.gems.education.infrastructure.driving.rest.constants.RestConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class RegisterStudentRequest {

    @NotBlank(message = RestConstants.NAME_REQUIRED_MESSAGE)
    @Size(min = 2, max = 50, message = RestConstants.NAME_SIZE_MESSAGE)
    private String name;

    @NotBlank(message = RestConstants.EMAIL_REQUIRED_MESSAGE)
    @Email(message = RestConstants.EMAIL_VALID_MESSAGE)
    private String email;

    @PastOrPresent(message = RestConstants.BIRTHDATE_FUTURE_MESSAGE)
    private LocalDate birthDate;

    @NotBlank(message = RestConstants.COUNTRY_REQUIRED_MESSAGE)
    private String country;

    @NotBlank(message = RestConstants.CITY_REQUIRED_MESSAGE)
    private String city;

    @NotBlank(message = RestConstants.DOCUMENT_TYPE_REQUIRED_MESSAGE)
    private String documentType;

    @NotBlank(message = RestConstants.DOCUMENT_NUMBER_REQUIRED_MESSAGE)
    @Size(min = 5, max = 20, message = RestConstants.DOCUMENT_NUMBER_SIZE_MESSAGE)
    @Pattern(
            regexp = RestConstants.DOCUMENT_NUMBER_PATTERN_REGEX,
            message = RestConstants.DOCUMENT_NUMBER_PATTERN_MESSAGE
    )
    private String documentNumber;

    public RegisterStudentRequest() {
    }

    public RegisterStudentRequest(
            String name,
            String email,
            LocalDate birthDate,
            String country,
            String city,
            String documentType,
            String documentNumber
    ) {
        this.name = name;
        this.email = email;
        this.birthDate = birthDate;
        this.country = country;
        this.city = city;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}
