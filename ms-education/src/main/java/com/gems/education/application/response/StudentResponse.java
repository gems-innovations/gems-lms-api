package com.gems.education.application.response;

import com.gems.education.domain.values.BirthDate;
import com.gems.education.domain.values.City;
import com.gems.education.domain.values.Country;
import com.gems.education.domain.values.DocumentNumber;
import com.gems.education.domain.values.DocumentType;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.Name;
import com.gems.education.domain.values.StudentId;

import java.time.LocalDate;

public class StudentResponse {
  private final Long id;
  private final String name;
  private final String email;
  private final LocalDate birthDate;
  private final String country;
  private final String city;
  private final String documentType;
  private final String documentNumber;

  public StudentResponse(Long id, String name, String email, LocalDate birthDate, String country, String city, String documentType, String documentNumber) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.birthDate = birthDate;
    this.country = country;
    this.city = city;
    this.documentType = documentType;
    this.documentNumber = documentNumber;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getCountry() {
    return country;
  }

  public String getCity() {
    return city;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getDocumentNumber() {
    return documentNumber;
  }
}
