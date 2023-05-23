package com.example.shopordermicrobackend.shopordermicrobackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrderDetail;

public interface ShopOrderDetailRepository extends JpaRepository<ShopOrderDetail, Long>{
    
    List<ShopOrderDetail> findByOrderId(Long orderId);
    
}
