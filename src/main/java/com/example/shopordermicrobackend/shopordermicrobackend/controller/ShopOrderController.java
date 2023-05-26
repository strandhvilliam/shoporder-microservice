package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.shopordermicrobackend.shopordermicrobackend.model.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderDetailRepository;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpMethod;








@Tag(name = "ShopOrderController", description = "Endpoints for getting all orders and orders by id")
@RestController
public class ShopOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final ShopOrderDetailRepository shopOrderDetailRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShopOrderController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public ShopOrderController(ShopOrderRepository shopOrderRepository,
            ShopOrderDetailRepository shopOrderDetailRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.shopOrderDetailRepository = shopOrderDetailRepository;
    }

    @GetMapping("/orders")
    public List<ResponseObject> getOrders() {

        List<ShopOrder> shopOrders = shopOrderRepository.findAll();



        List<ResponseObject> responseArray = new ArrayList<>();

        for (ShopOrder shopOrder : shopOrders) {
            ResponseObject responseObject = parseOrder(shopOrder);
            responseArray.add(responseObject);
        }

        return responseArray;

    }

    @GetMapping("/orders/{id}")
    public ResponseObject getOrderById(@PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).get();

        return parseOrder(shopOrder);
    }

    @PostMapping("/orders")
    public ShopOrder postOrders(@RequestBody ShopOrder body) {
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

    public ResponseObject parseOrder(ShopOrder shopOrder) {

        List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());

        shopOrder.setShopOrderDetails(shopOrderDetails);

        Long customerId = shopOrder.getCustomerId();

        String customerString = restTemplate.getForObject("http://customerservice:8080/customers/" + customerId,
                String.class);

        JSONObject customerJsonObject = new JSONObject(customerString);

        Customer customer = new Customer(customerJsonObject.getLong("id"), customerJsonObject.getString("name"),
                customerJsonObject.getString("ssn"));

        List<Item> items = new ArrayList<>();

        for (ShopOrderDetail detail : shopOrder.getShopOrderDetails()) {
            Long itemId = detail.getItemId();

            String itemString = restTemplate.getForObject("http://itemservice:8080/item/" + itemId, String.class);

            JSONObject itemJsonObject = new JSONObject(itemString);

            Item item = new Item(itemJsonObject.getLong("id"), itemJsonObject.getString("name"),
                    itemJsonObject.getInt("price"));

            items.add(item);
        }

        return new ResponseObject(shopOrder.getId(), shopOrder.getOrderDate(), customer, items);

    }
}




/*
 * 
 List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
//        shopOrder.setShopOrderDetails(shopOrderDetails);
//
//        Long customerId = shopOrder.getCustomerId();
//
//        String customerString = restTemplate.getForObject("http://customerservice:8080/customers/" + customerId,
//                String.class);
//
//        JSONObject customerJsonObject = new JSONObject(customerString);
//
//        Customer customer = new Customer(customerJsonObject.getLong("id"), customerJsonObject.getString("name"),
//                customerJsonObject.getString("ssn"));
//
//        JSONObject responseObject = new JSONObject();
//
//        responseObject.put("id", shopOrder.getId());
//        responseObject.put("orderDate", shopOrder.getOrderDate());
//        JSONObject customerObject = new JSONObject();
//        customerObject.put("id", customer.id);
//        customerObject.put("name", customer.name);
//        customerObject.put("ssn", customer.ssn);
//        responseObject.put("customer", customerObject);
//
//        JSONArray orderedItems = new JSONArray();
//
//        for (ShopOrderDetail detail : shopOrder.getShopOrderDetails()) {
//            Long itemId = detail.getItemId();
//            logger.info("itemId: " + itemId);
//
//            String itemString = restTemplate.getForObject("http://itemservice:8080/item/" + itemId, String.class);
//
//            JSONObject itemJsonObject = new JSONObject(itemString);
//
//            Item item = new Item(itemJsonObject.getLong("id"), itemJsonObject.getString("name"),
//                    itemJsonObject.getInt("price"));
//
//            if (item != null) {
//                JSONObject itemObject = new JSONObject();
//                itemObject.put("id", item.id);
//                itemObject.put("name", item.name);
//                itemObject.put("price", item.price);
//                orderedItems.put(itemObject);
//            }
//
//            responseObject.put("items", orderedItems);
//        }
//
//        return responseObject;
 * 
 * 
 * 
 * 
 */
