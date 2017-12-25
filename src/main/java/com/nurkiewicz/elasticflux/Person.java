package com.nurkiewicz.elasticflux;

import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

@Value
class Person {
    @NotNull
    @Valid
    private final Address address;

    @NotBlank
    private final String firstName;

    private final String middleName;

    @NotBlank
    private final String lastName;

    @NotBlank
    private final String email;

    private final String companyEmail;

    @NotBlank
    private final String username;

    @NotBlank
    private final String password;

    @NotBlank
    private final String sex;

    @NotBlank
    private final String telephoneNumber;

    @NotNull
    private final LocalDate dateOfBirth;

    @Valid
    private final Company company;

    @NotBlank
    private final String nationalIdentityCardNumber;

    private final String nationalIdentificationNumber;

    private final String passportNumber;

}

@Value
class Address {
    @NotBlank
    private final String street;

    @NotBlank
    private final String streetNumber;

    private final String apartmentNumber;

    @NotBlank
    private final String postalCode;

    @NotBlank
    private final String city;

    private final List<String> lines;
}

@Value
class Company {
    @NotBlank
    private final String name;

    @NotBlank
    private final String domain;

    @NotBlank
    private final String email;

    @NotNull
    private final URL url;

    @NotBlank
    private final String vatIdentificationNumber;

}