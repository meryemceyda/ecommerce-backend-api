package com.meryem.ecommerce.controller;

import com.meryem.ecommerce.dto.OrderResponseDto;
import com.meryem.ecommerce.dto.UserRequestDto;
import com.meryem.ecommerce.dto.UserResponseDto;
import com.meryem.ecommerce.service.OrderService;
import com.meryem.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(
        name = "Users",
        description = "User management endpoints"
)
public class UserController {
    private final UserService userService;
    private final OrderService orderService;

    public UserController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }
    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }
    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    @PostMapping
    public UserResponseDto createUser(
            @Valid @RequestBody UserRequestDto dto) {

        return userService.createUser(dto);
    }
    @PutMapping("/{id}")
    public UserResponseDto updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto dto) {

        return userService.updateUser(id, dto);
    }
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping("/{id}/orders")
    @Operation(
            summary = "Get orders by user",
            description = "Returns all orders belonging to the specified user."
    )
    public ResponseEntity<List<OrderResponseDto>> getUserOrders(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.getOrdersByUserId(id)
        );
    }
}
