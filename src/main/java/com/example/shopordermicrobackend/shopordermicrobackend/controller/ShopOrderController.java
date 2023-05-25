package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrder;
import com.example.shopordermicrobackend.shopordermicrobackend.model.ShopOrderDetail;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderDetailRepository;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpMethod;

class Customer {
    protected Long id;
    protected String name;
    protected String ssn;

    Customer(Long id, String name, String ssn) {
        this.id = id;
        this.name = name;
        this.ssn = ssn;
    }

}

class Item {
    protected Long id;
    protected String name;
    protected Integer price;

    Item(Long id, String name, Integer price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}

@RestController
public class ShopOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final ShopOrderDetailRepository shopOrderDetailRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShopOrderController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @GetMapping("/orders2")
    public String getOrders2() throws JsonProcessingException {

        RestTemplate restTemplate = new RestTemplate();

        List<ShopOrder> shopOrders = shopOrderRepository.findAll();

        JSONArray jsonArray = new JSONArray();

        for (ShopOrder shopOrder : shopOrders) {
            List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
            shopOrder.setShopOrderDetails(shopOrderDetails);

            Long customerId = shopOrder.getCustomerId();
            
            String customerString = restTemplate.getForObject("http://customerservice:8080/customers/" + customerId, String.class);

            JSONObject customerJsonObject = new JSONObject(customerString);

            Customer customer = new Customer(customerJsonObject.getLong("id"), customerJsonObject.getString("name"),
                    customerJsonObject.getString("ssn"));


            JSONObject responseObject = new JSONObject();

            responseObject.put("id", shopOrder.getId());
            responseObject.put("orderDate", shopOrder.getOrderDate());
            JSONObject customerObject = new JSONObject();
            customerObject.put("id", customer.id);
            customerObject.put("name", customer.name);
            customerObject.put("ssn", customer.ssn);
            responseObject.put("customer", customerObject);

            JSONArray orderedItems = new JSONArray();

            for (ShopOrderDetail detail : shopOrder.getShopOrderDetails()) {
                Long itemId = detail.getItemId();
                logger.info("itemId: " + itemId);
            
                String itemString = restTemplate.getForObject("http://itemservice:8080/item/" + itemId, String.class);

                JSONObject itemJsonObject = new JSONObject(itemString);

                Item item = new Item(itemJsonObject.getLong("id"), itemJsonObject.getString("name"),
                        itemJsonObject.getInt("price"));

                if (item != null) {
                    JSONObject itemObject = new JSONObject();
                    itemObject.put("id", item.id);
                    itemObject.put("name", item.name);
                    itemObject.put("price", item.price);
                    orderedItems.put(itemObject);
                }
            }

            responseObject.put("items", orderedItems);
            jsonArray.put(responseObject);
        }

        return jsonArray.toString();

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
