package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public String getOrders2() throws JsonMappingException, JsonProcessingException, JSONException {

        RestTemplate restTemplate = new RestTemplate();

        String cHost = System.getenv("CUSTOMER_HOST");
        String cPort = System.getenv("CUSTOMER_PORT");
        String pHost = System.getenv("PRODUCT_HOST");
        String pPort = System.getenv("PRODUCT_PORT");

        String result = restTemplate.getForObject("http://" + cHost + ":" + cPort + "/customers", String.class);
        String result2 = restTemplate.getForObject("http://" + pHost + ":" + pPort + "/products", String.class);

        logger.info("RESULT: " + result);
        logger.info("RESULT2: " + result2);

        List<ShopOrder> shopOrders = shopOrderRepository.findAll();

        JSONArray jsonArray = new JSONArray();

        for (ShopOrder shopOrder : shopOrders) {
            List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
            shopOrder.setShopOrderDetails(shopOrderDetails);

            String customerId = shopOrder.getCustomerId().toString();
            logger.info("customerId: " + customerId);

            String customer = restTemplate.getForObject("http://" + cHost + ":" + cPort + "/customers/" + customerId,
                    String.class);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", shopOrder.getId());
            jsonObject.put("orderDate", shopOrder.getOrderDate());
            jsonObject.put("customer", objectMapper.readTree(customer));
            JSONArray details = new JSONArray();

            for (ShopOrderDetail detail : shopOrder.getShopOrderDetails()) {
                String productId = detail.getProductId().toString();
                logger.info("productId: " + productId);
                String product = restTemplate.getForObject("http://" + pHost + ":" + pPort + "/products/" + productId,
                        String.class);
                details.put(product);
            }

            jsonObject.put("products", details);
            jsonArray.put(jsonObject);

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
