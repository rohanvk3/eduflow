package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "institute_settings")
public class InstituteSettings {
    @Id
    public Long id = 1L;
    @Column(nullable = false, length = 150)
    public String instituteName = "EduFlow";
    @Column(nullable = false, length = 30)
    public String instituteCode = "EDUFLOW";
    @Column(length = 254)
    public String email;
    @Column(length = 16)
    public String phoneNumber;
    @Column(length = 16)
    public String alternatePhone;
    @Column(length = 250)
    public String addressLine1;
    @Column(length = 250)
    public String addressLine2;
    @Column(length = 100)
    public String city;
    @Column(length = 100)
    public String state;
    @Column(length = 20)
    public String postalCode;
    @Column(length = 100)
    public String country = "India";
    @Column(length = 300)
    public String website;
    @Column(nullable = false, length = 80)
    public String timezone = "Asia/Kolkata";
    @Column(nullable = false, length = 3)
    public String currencyCode = "INR";
    public LocalDate academicYearStart;
    public LocalDate academicYearEnd;
    @Column(length = 80)
    public String logoStorageKey;
    @Column(length = 40)
    public String logoContentType;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    @Version
    public long version;
}
