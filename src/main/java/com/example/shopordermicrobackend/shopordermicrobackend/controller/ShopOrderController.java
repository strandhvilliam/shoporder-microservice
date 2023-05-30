package com.example.shopordermicrobackend.shopordermicrobackend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.example.shopordermicrobackend.shopordermicrobackend.exception.CustomerNotFoundException;
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
    public ResponseEntity<List<ShopOrderResponse>> getOrders(@RequestHeader String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        List<ShopOrderResponse> responseArray = shopOrderRepository
                .findAll()
                .stream()
                .map(shopOrder -> parseOrder(shopOrder, headers))
                .toList();
        return new ResponseEntity<>(responseArray, HttpStatus.OK);
    }

    @Operation(summary = "Gets order by id")
    @GetMapping("/orders/{id}")
    public ShopOrderResponse getOrderById(@Min(0) @PathVariable Long id, @RequestHeader String authorization) {
        ShopOrder shopOrder = shopOrderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        return parseOrder(shopOrder, headers);
    }

    @Operation(summary = "Creates an order")
    @PostMapping("/orders")
    public ResponseEntity<ShopOrder> postOrders(@Valid @RequestBody ShopOrder body, @RequestHeader String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        logger.info("Customer id: {}", body.getCustomerId());
        try {
            Objects.requireNonNull(restTemplate.exchange(
                    "http://customerservice:8080/customers/" + body.getCustomerId(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Customer.class
            ).getBody());

        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error(e.getMessage());
            throw new InvalidAuthException();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomerNotFoundException(body.getCustomerId());
        }

        ShopOrder savedOrder = shopOrderRepository.save(body);

        logger.info("Order created with id {}", savedOrder.getId());

        List<ShopOrderDetail> shopOrderDetails = body.getShopOrderDetails();

        boolean hasDetailViolations = shopOrderDetails.stream()
                .map(detail -> validator.validate(detail))
                .anyMatch(violations -> !violations.isEmpty());

        if (hasDetailViolations) {
            logger.error("ShopOrderDetail validation failed");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        shopOrderDetails.forEach(shopOrderDetail -> shopOrderDetail.setOrderId(savedOrder.getId()));
        shopOrderDetailRepository.saveAll(shopOrderDetails);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);

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
    public String deleteOrder(@Min(0) @PathVariable Long id) {
        shopOrderRepository.deleteById(id);

        List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(id);
        shopOrderDetailRepository.deleteAll(shopOrderDetails);

        return "Order deleted";
    }

    public ShopOrderResponse parseOrder(ShopOrder shopOrder, HttpHeaders headers) {

        List<ShopOrderDetail> shopOrderDetails = shopOrderDetailRepository.findByOrderId(shopOrder.getId());
        shopOrder.setShopOrderDetails(shopOrderDetails);
        Long customerId = shopOrder.getCustomerId();

        JSONObject customerJsonObject;

        try {
            String customerString = restTemplate.exchange(
                    "http://customerservice:8080/customers/" + customerId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers), String.class).getBody();
            customerJsonObject = new JSONObject(customerString);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new InvalidAuthException();
        }

        Customer customer = new Customer(
                customerJsonObject.getLong("id"),
                customerJsonObject.getString("name"),
                customerJsonObject.getString("ssn"));


        List<Item> items = shopOrder.getShopOrderDetails().stream()
                .map(ShopOrderDetail::getItemId)
                .map(itemId -> restTemplate.getForObject("http://itemservice:8080/item/" + itemId, String.class))
                .map(JSONObject::new)
                .map(itemJsonObject -> new Item(itemJsonObject.getLong("id"), itemJsonObject.getString("name"),
                        itemJsonObject.getInt("price")))
                .toList();


        return new ShopOrderResponse(
                shopOrder.getId(),
                shopOrder.getOrderDate(),
                customer,
                items
        );

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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({CustomerNotFoundException.class})
    public ErrorResponse handleCustomerNotFoundException(CustomerNotFoundException e) {
        return new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND, LocalDateTime.now());
    }
}
