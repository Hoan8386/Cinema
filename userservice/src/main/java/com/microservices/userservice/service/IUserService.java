package com.microservices.userservice.service;

import java.util.List;

import com.microservices.userservice.dto.CreateUserRequestDTO;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.UserResponseDTO;
import com.microservices.userservice.dto.identity.TokenExchangeResponse;

public interface IUserService {
    UserResponseDTO createUser(CreateUserRequestDTO dto);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(String id);

    UserResponseDTO updateUser(String id, CreateUserRequestDTO dto);

    void deleteUser(String id);

    TokenExchangeResponse login(LoginRequestDto dto);
}
