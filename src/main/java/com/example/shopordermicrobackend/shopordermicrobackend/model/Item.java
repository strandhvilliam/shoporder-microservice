package com.example.shopordermicrobackend.shopordermicrobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Item {
    private Long id;
    private String name;
    private Integer price;


}
