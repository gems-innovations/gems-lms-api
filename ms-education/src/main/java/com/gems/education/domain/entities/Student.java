package com.gems.education.domain.entities;

import com.gems.education.domain.values.BirthDate;
import com.gems.education.domain.values.City;
import com.gems.education.domain.values.Country;
import com.gems.education.domain.values.DocumentNumber;
import com.gems.education.domain.values.DocumentType;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.Name;
import com.gems.education.domain.values.StudentId;

import java.time.LocalDate;
import java.util.Objects;

public class Student {
  private final StudentId id;
  private final Name name;
  private final Email email;
  private final BirthDate birthDate;
  private final Country country;
  private final City city;
  private final DocumentType documentType;
  private final DocumentNumber documentNumber;

  public Student(
    Long id,
    String name,
    String email,
    LocalDate birthDate,
    String country,
    String city,
    String documentType,
    String documentNumber
  ) {
    this.id = id != null ? new StudentId(id) : null;
    this.name = new Name(name);
    this.email = new Email(email);
    this.birthDate = new BirthDate(birthDate);
    this.country = new Country(country);
    this.city = new City(city);
    this.documentType = DocumentType.fromString(documentType);
    this.documentNumber = new DocumentNumber(documentNumber);
  }

  public Student(
    String name,
    String email,
    LocalDate birthDate,
    String country,
    String city,
    String documentType,
    String documentNumber
  ) {
    this(null, name, email, birthDate, country, city, documentType, documentNumber);
  }

  public StudentId getId() {
    return id;
  }

  public Name getName() {
    return name;
  }

  public Email getEmail() {
    return email;
  }

  public BirthDate getBirthDate() {
    return birthDate;
  }

  public Country getCountry() {
    return country;
  }

  public City getCity() {
    return city;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }

  public DocumentNumber getDocumentNumber() {
    return documentNumber;
  }

  public Student updateEmail(String newEmail) {
    return new Student(
      id != null ? id.getValue() : null,
      name.getValue(),
      newEmail,
      birthDate.getValue(),
      country.getValue(),
      city.getValue(),
      documentType.name(),
      documentNumber.getValue()
    );
  }

  public Student updateCity(String newCity) {
    return new Student(
      id != null ? id.getValue() : null,
      name.getValue(),
      email.getValue(),
      birthDate.getValue(),
      country.getValue(),
      newCity,
      documentType.name(),
      documentNumber.getValue()
    );
  }

  public Student updateCountry(String newCountry) {
    return new Student(
      id != null ? id.getValue() : null,
      name.getValue(),
      email.getValue(),
      birthDate.getValue(),
      newCountry,
      city.getValue(),
      documentType.name(),
      documentNumber.getValue()
    );
  }
}