package com.meryem.ecommerce.controller;

import com.meryem.ecommerce.dto.OrderRequestDto;
import com.meryem.ecommerce.dto.OrderResponseDto;
import com.meryem.ecommerce.dto.OrderStatusUpdateRequestDto;
import com.meryem.ecommerce.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long currentUserId = Long.valueOf(jwt.getSubject());

        OrderResponseDto response =
                orderService.createOrder(dto, currentUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long currentUserId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                orderService.getOrdersByUserId(currentUserId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long currentUserId = Long.valueOf(jwt.getSubject());

        boolean isAdmin = jwt.getClaimAsString("role")
                .equals("ADMIN");

        return ResponseEntity.ok(
                orderService.getOrderById(
                        id,
                        currentUserId,
                        isAdmin
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequestDto dto
    ) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(id, dto)
        );
    }
}