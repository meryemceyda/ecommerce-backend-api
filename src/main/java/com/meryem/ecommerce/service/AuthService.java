
package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.LoginRequestDto;
import com.meryem.ecommerce.dto.RegisterRequestDto;
import com.meryem.ecommerce.dto.RegisterResponseDto;
import com.meryem.ecommerce.entity.Role;
import com.meryem.ecommerce.entity.user;
import com.meryem.ecommerce.exception.EmailAlreadyExistsException;
import com.meryem.ecommerce.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.meryem.ecommerce.dto.AuthResponseDto;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email address is already registered"
            );
        }

        user newUser = new user();

        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());

        newUser.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        newUser.setRole(Role.USER);

        user savedUser = userRepository.save(newUser);

        return new RegisterResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail()
        );
    }


    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {

        user existingUser = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                existingUser.getPassword()
        );

        if (!passwordMatches) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(existingUser);

        return new AuthResponseDto(token, "Bearer");
    }

}
