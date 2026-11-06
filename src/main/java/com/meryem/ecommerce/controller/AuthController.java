package com.meryem.ecommerce.controller;

import com.meryem.ecommerce.dto.LoginRequestDto;
import com.meryem.ecommerce.dto.RegisterRequestDto;
import com.meryem.ecommerce.dto.RegisterResponseDto;
import com.meryem.ecommerce.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.meryem.ecommerce.dto.AuthResponseDto;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request) {

        RegisterResponseDto response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}