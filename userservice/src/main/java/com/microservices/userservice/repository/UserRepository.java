package com.microservices.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.microservices.userservice.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
