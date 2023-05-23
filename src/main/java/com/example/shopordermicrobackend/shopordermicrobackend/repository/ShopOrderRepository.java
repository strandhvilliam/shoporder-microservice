package com.example.shopordermicrobackend.shopordermicrobackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrder;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    
}
