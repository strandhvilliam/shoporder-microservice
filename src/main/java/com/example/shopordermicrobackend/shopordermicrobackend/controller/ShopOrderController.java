package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.shopordermicrobackend.shopordermicrobackend.exception.ErrorResponse;
import com.example.shopordermicrobackend.shopordermicrobackend.exception.InvalidAuthException;
import com.example.shopordermicrobackend.shopordermicrobackend.exception.OrderNotFoundException;
import com.example.shopordermicrobackend.shopordermicrobackend.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.*;
import jakarta.validation.constraints.Min;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderDetailRepository;
import com.example.shopordermicrobackend.shopordermicrobackend.repository.ShopOrderRepository;

@Tag(name = "ShopOrderController", description = "Endpoints for getting all orders and orders by id")
@RestController
@Validated
public class ShopOrderController {

    private final ShopOrderRepository shopOrderRepository;
    private final ShopOrderDetailRepository shopOrderDetailRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShopOrderController.class);
    private final RestTemplate restTemplate;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public ShopOrderController(
            ShopOrderRepository shopOrderRepository,
            ShopOrderDetailRepository shopOrderDetailRepository,
            RestTemplate restTemplate) {
        this.shopOrderRepository = shopOrderRepository;
        this.shopOrderDetailRepository = shopOrderDetailRepository;
        this.restTemplate = restTemplate;
    }

    @Operation(summary = "Gets all orders")
    @GetMapping("/orders")
    public ResponseEntity<List<ResponseObject>> getOrders(@RequestHeader String authorization) {

        logger.info("Authorization header: " + authorization);

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", authorization);

        List<ShopOrder> shopOrders = shopOrderRepository.findAll();

        List<ResponseObject> responseArray = new ArrayList<>();

        for (ShopOrder shopOrder : shopOrders) {
            ResponseObject responseObject = parseOrder(shopOrder, headers);
            responseArray.add(responseObject);
        }

        return new ResponseEntity<>(responseArray, HttpStatus.OK);

    }

    @Operation(summary = "Gets order by id")
    @GetMapping("/orders/{id}")
    public ResponseObject getOrderById(@Min(0) @PathVariable Long id, @RequestHeader String authorization) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", authorization);

        return parseOrder(shopOrder, headers);
    }

    @Operation(summary = "Creates an order")
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

    @Operation(summary = "Updates an order")
    @PutMapping("/orders/{id}")
    public ShopOrder putOrder(@Valid @RequestBody ShopOrder body, @Min(0) @PathVariable Long id) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        shopOrder.setCustomerId(body.getCustomerId());
        shopOrder.setOrderDate(body.getOrderDate());
        shopOrderRepository.save(shopOrder);
        return shopOrder;
    }


    @Operation(summary = "Deletes an order")
    @DeleteMapping("/orders/{id}")
    public void deleteOrder(@Min(0) @PathVariable Long id) {
        shopOrderRepository.deleteById(id);
    }

    public ResponseObject parseOrder(ShopOrder shopOrder, HttpHeaders headers) {

        List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
        shopOrder.setShopOrderDetails(shopOrderDetails);
        Long customerId = shopOrder.getCustomerId();

        JSONObject customerJsonObject;

        try {
            String customerString = restTemplate.exchange("http://customerservice:8080/customers/" + customerId, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
            customerJsonObject = new JSONObject(customerString);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidAuthException();
        }

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

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({InvalidAuthException.class})
    public ErrorResponse handleInvalidAuthException(InvalidAuthException e) {
        return new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED, LocalDateTime.now());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({OrderNotFoundException.class})
    public ErrorResponse handleOrderNotFoundException(OrderNotFoundException e) {
        return new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND, LocalDateTime.now());
    }
}
