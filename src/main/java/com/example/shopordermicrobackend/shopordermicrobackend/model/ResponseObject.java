package com.example.shopordermicrobackend.shopordermicrobackend.model;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class ResponseObject {

    private Long id;
    private Date orderDate;
    private Customer customer;
    private List<Item> items;

}
