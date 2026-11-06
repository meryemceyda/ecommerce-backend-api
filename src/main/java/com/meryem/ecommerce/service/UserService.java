package com.meryem.ecommerce.service;

import com.meryem.ecommerce.dto.UserRequestDto;
import com.meryem.ecommerce.dto.UserResponseDto;
import com.meryem.ecommerce.entity.user;
import com.meryem.ecommerce.exception.ResourceNotFoundException;
import com.meryem.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public UserResponseDto getUserById(Long id) {
        user user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        return mapToResponseDto(user);
    }

    public UserResponseDto createUser(UserRequestDto dto) {
        user user = new user();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        user savedUser = userRepository.save(user);

        return mapToResponseDto(savedUser);
    }

    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        user user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        user updatedUser = userRepository.save(user);

        return mapToResponseDto(updatedUser);
    }

    public void deleteUser(Long id) {
        user user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        userRepository.delete(user);
    }

    private UserResponseDto mapToResponseDto(user user) {
        UserResponseDto responseDto = new UserResponseDto();

        responseDto.setId(user.getId());
        responseDto.setFirstName(user.getFirstName());
        responseDto.setLastName(user.getLastName());
        responseDto.setEmail(user.getEmail());

        return responseDto;
    }
}