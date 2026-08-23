package com.infy.claims.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Customer {
    private String id;
    private String name;
    private String gender;      // M | F | O
    private LocalDate dob;
    private String pincode;
    private String occupation;
    private String loyaltyTier; // SILVER | GOLD | PLATINUM
}
