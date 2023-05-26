package com.example.shopordermicrobackend.shopordermicrobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Customer {
    private Long id;
    private String name;
    private String ssn;
}
