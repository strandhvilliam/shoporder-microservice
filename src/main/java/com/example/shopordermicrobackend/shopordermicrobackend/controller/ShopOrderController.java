package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrder;
import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrderDetail;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderDetailRepository;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderRepository;

@RestController
public class ShopOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final ShopOrderDetailRepository shopOrderDetailRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShopOrderController.class);

    public ShopOrderController(ShopOrderRepository shopOrderRepository,
            ShopOrderDetailRepository shopOrderDetailRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.shopOrderDetailRepository = shopOrderDetailRepository;
    }

    @GetMapping("/orders")
    public List<ShopOrder> getOrders() {
        List<ShopOrder> shopOrders = shopOrderRepository.findAll();

        for (ShopOrder shopOrder : shopOrders) {
            List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
            shopOrder.setShopOrderDetails(shopOrderDetails);
        }

        return shopOrders;
    }

    @GetMapping("/orders/{id}")
    public ShopOrder getOrderById(@PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).get();
        List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(id);
        shopOrder.setShopOrderDetails(shopOrderDetails);
        return shopOrder;
    }

    @PostMapping("/orders")
    public ShopOrder postOrders(@RequestBody ShopOrder body) {

        logger.info("body: " + body);

        ShopOrder savedOrder = shopOrderRepository.save(body);

        List<ShopOrderDetail> shopOrderDetails = body.getShopOrderDetails();

        for (ShopOrderDetail shopOrderDetail : shopOrderDetails) {
            shopOrderDetail.setOrderId(savedOrder.getId());
            shopOrderDetailRepository.save(shopOrderDetail);
        }

        return savedOrder;

    }

    @PutMapping("/orders/{id}")
    public ShopOrder putOrder(@RequestBody ShopOrder body, @PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).get();
        shopOrder.setCustomerId(body.getCustomerId());
        shopOrder.setOrderDate(body.getOrderDate());
        shopOrderRepository.save(shopOrder);
        return shopOrder;
    }

    @DeleteMapping("/orders/{id}")
    public void deleteOrder(@PathVariable Long id) {
        shopOrderRepository.deleteById(id);
    }

}
