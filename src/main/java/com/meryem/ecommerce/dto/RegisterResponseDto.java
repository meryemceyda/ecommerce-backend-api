package com.meryem.ecommerce.dto;

public record RegisterResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}