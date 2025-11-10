package com.gems.education.domain.entities;

import com.gems.education.domain.values.BirthDate;
import com.gems.education.domain.values.City;
import com.gems.education.domain.values.Country;
import com.gems.education.domain.values.DocumentNumber;
import com.gems.education.domain.values.DocumentType;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.Name;
import com.gems.education.domain.values.StudentId;

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
            StudentId id,
            Name name,
            Email email,
            BirthDate birthDate,
            Country country,
            City city,
            DocumentType documentType,
            DocumentNumber documentNumber
    ) {
        this.id = Objects.requireNonNull(id, "Student ID cannot be null");
        this.name = Objects.requireNonNull(name, "Student name cannot be null");
        this.email = Objects.requireNonNull(email, "Student email cannot be null");
        this.birthDate = Objects.requireNonNull(birthDate, "Birth date cannot be null");
        this.country = Objects.requireNonNull(country, "Country cannot be null");
        this.city = Objects.requireNonNull(city, "City cannot be null");
        this.documentType = Objects.requireNonNull(documentType, "Document type cannot be null");
        this.documentNumber = Objects.requireNonNull(documentNumber, "Document number cannot be null");
    }

    public Student(
            Name name,
            Email email,
            BirthDate birthDate,
            Country country,
            City city,
            DocumentType documentType,
            DocumentNumber documentNumber
    ) {
        this.id = null;
        this.name = Objects.requireNonNull(name, "Student name cannot be null");
        this.email = Objects.requireNonNull(email, "Student email cannot be null");
        this.birthDate = Objects.requireNonNull(birthDate, "Birth date cannot be null");
        this.country = Objects.requireNonNull(country, "Country cannot be null");
        this.city = Objects.requireNonNull(city, "City cannot be null");
        this.documentType = Objects.requireNonNull(documentType, "Document type cannot be null");
        this.documentNumber = Objects.requireNonNull(documentNumber, "Document number cannot be null");
    }

    public StudentId getValue() { return id; }
    public Name getName() { return name; }
    public Email getEmail() { return email; }
    public BirthDate getBirthDate() { return birthDate; }
    public Country getCountry() { return country; }
    public City getCity() { return city; }
    public DocumentType getDocumentType() { return documentType; }
    public DocumentNumber getDocumentNumber() { return documentNumber; }

    public Student updateEmail(Email newEmail) {
        return new Student(
                this.id,
                this.name,
                newEmail,
                this.birthDate,
                this.country,
                this.city,
                this.documentType,
                this.documentNumber
        );
    }

    public Student updateCity(City newCity) {
        return new Student(
                this.id,
                this.name,
                this.email,
                this.birthDate,
                this.country,
                newCity,
                this.documentType,
                this.documentNumber
        );
    }

    public Student updateCountry(Country newCountry) {
        return new Student(
                this.id,
                this.name,
                this.email,
                this.birthDate,
                newCountry,
                this.city,
                this.documentType,
                this.documentNumber
        );
    }
}
