package com.example.shopordermicrobackend.shopordermicrobackend.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Item {

    @NotNull
    @Min(0)
    private Long id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    @NotNull
    @Min(0)
    private Integer price;


}
