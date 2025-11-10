package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("students")
public class StudentEntity {
    @Id
    @Column("student_id")
    private Long id;

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    @Column("birth_date")
    private java.time.LocalDate birthDate;

    @Column("country")
    private String country;

    @Column("city")
    private String city;

    @Column("document_type")
    private String documentType;

    @Column("document_number")
    private String documentNumber;

    public StudentEntity() {
    }

    public StudentEntity(
            Long id,
            String name,
            String email,
            java.time.LocalDate birthDate,
            String country,
            String city,
            String documentType,
            String documentNumber
    ) {
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

    public void setId(Long id) {
        this.id = id;
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

    public java.time.LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(java.time.LocalDate birthDate) {
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

    @Override
    public String toString() {
        return String.format(
                "StudentEntity[id=%s, name=%s, email=%s, documentType=%s, documentNumber=%s, city=%s, country=%s]",
                id, name, email, documentType, documentNumber, city, country
        );
    }
}
