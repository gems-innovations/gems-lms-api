package com.gems.education.application.command;

import java.time.LocalDate;

public class StudentCommand {
  private final String name;
  private final String email;
  private final LocalDate birthDate;
  private final String country;
  private final String city;
  private final String documentType;
  private final String documentNumber;

  public StudentCommand(String name, String email, LocalDate birthDate, String country, String city, String documentType, String documentNumber) {
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
