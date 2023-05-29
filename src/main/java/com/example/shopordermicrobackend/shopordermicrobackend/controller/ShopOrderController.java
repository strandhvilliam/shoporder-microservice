package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.shopordermicrobackend.shopordermicrobackend.model.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.*;
import jakarta.validation.constraints.Min;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderDetailRepository;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;








@Tag(name = "ShopOrderController", description = "Endpoints for getting all orders and orders by id")
@RestController
@Validated
public class ShopOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final ShopOrderDetailRepository shopOrderDetailRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShopOrderController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public ShopOrderController(ShopOrderRepository shopOrderRepository,
            ShopOrderDetailRepository shopOrderDetailRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.shopOrderDetailRepository = shopOrderDetailRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ResponseObject>> getOrders() {


        List<ShopOrder> shopOrders = shopOrderRepository.findAll();

        List<ResponseObject> responseArray = new ArrayList<>();

        try {
            for (ShopOrder shopOrder : shopOrders) {
                ResponseObject responseObject = parseOrder(shopOrder);
                responseArray.add(responseObject);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }

        return new ResponseEntity<>(responseArray, HttpStatus.OK);

    }

    @GetMapping("/orders/{id}")
    public ResponseObject getOrderById(@Min(0) @PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).get();

        return parseOrder(shopOrder);
    }

    @PostMapping("/orders")
    public ResponseEntity<ShopOrder> postOrders(@Valid @RequestBody ShopOrder body) {
        ShopOrder savedOrder = shopOrderRepository.save(body);

        List<ShopOrderDetail> shopOrderDetails = body.getShopOrderDetails();

        for (ShopOrderDetail shopOrderDetail : shopOrderDetails) {

            Set<ConstraintViolation<ShopOrderDetail>> detailViolations = validator.validate(shopOrderDetail);

            if (!detailViolations.isEmpty()) {
                logger.error("ShopOrderDetail validation failed");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            shopOrderDetail.setOrderId(savedOrder.getId());
            shopOrderDetailRepository.save(shopOrderDetail);
        }

        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);

    }
    @PutMapping("/orders/{id}")
    public ShopOrder putOrder(@Valid @RequestBody ShopOrder body, @Min(0) @PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).get();
        shopOrder.setCustomerId(body.getCustomerId());
        shopOrder.setOrderDate(body.getOrderDate());
        shopOrderRepository.save(shopOrder);
        return shopOrder;
    }

    @DeleteMapping("/orders/{id}")
    public void deleteOrder(@Min(0) @PathVariable Long id) {
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

        Set<ConstraintViolation<Customer>> customerViolations = validator.validate(customer);
        if (!customerViolations.isEmpty()) {
            throw new ConstraintViolationException(customerViolations);
        }



        List<Item> items = new ArrayList<>();

        for (ShopOrderDetail detail : shopOrder.getShopOrderDetails()) {
            Long itemId = detail.getItemId();

            String itemString = restTemplate.getForObject("http://itemservice:8080/item/" + itemId, String.class);

            JSONObject itemJsonObject = new JSONObject(itemString);

            Item item = new Item(itemJsonObject.getLong("id"), itemJsonObject.getString("name"),
                    itemJsonObject.getInt("price"));


            Set<ConstraintViolation<Item>> itemViolations = validator.validate(item);
            if (!itemViolations.isEmpty()) {
                throw new ConstraintViolationException(itemViolations);
            }

            items.add(item);
        }

        return new ResponseObject(shopOrder.getId(), shopOrder.getOrderDate(), customer, items);

    }
}
