package com.meryem.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequestDto {

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid OrderItemRequestDto> items;

    public OrderRequestDto() {
    }

    public List<OrderItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDto> items) {
        this.items = items;
    }
}