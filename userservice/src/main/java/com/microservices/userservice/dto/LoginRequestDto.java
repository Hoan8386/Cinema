package com.microservices.userservice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho request đăng nhập.
 * 
 * Được sử dụng trong endpoint:
 * POST /api/v1/public/login
 * 
 * Client gửi username và password để xác thực với Keycloak
 * và nhận JWT tokens (access_token, refresh_token, id_token).
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@Builder
public class LoginRequestDto {

    /** Username dùng để đăng nhập (bắt buộc) */
    String username;

    /** Password của user (bắt buộc) */
    String password;
}