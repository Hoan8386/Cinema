package com.microservices.userservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.identity.TokenExchangeResponse;
import com.microservices.userservice.service.IUserService;

/**
 * REST Controller cho các endpoint PUBLIC - KHÔNG yêu cầu authentication.
 * 
 * Controller này xử lý các thao tác mà người dùng chưa đăng nhập có thể thực
 * hiện,
 * chủ yếu là đăng nhập để lấy access token.
 * 
 * Base URL: /api/v1/public
 * 
 * Security: Các endpoint này KHÔNG được bảo vệ bởi Spring Security.
 * 
 * @author User Service Team
 * @version 1.0
 */
@RestController
@RequestMapping("api/v1/public")
public class PublicController {

    @Autowired
    private IUserService userService;

    /**
     * Đăng nhập người dùng và lấy JWT tokens.
     * 
     * Endpoint này:
     * 1. Nhận username và password từ client
     * 2. Gửi đến Keycloak để xác thực (OAuth2 Password Grant)
     * 3. Trả về JWT tokens nếu credentials hợp lệ:
     * - access_token: Dùng để gọi protected APIs (expire sau 5-15 phút)
     * - refresh_token: Dùng để lấy access_token mới (expire sau 30 phút)
     * - id_token: Chứa thông tin user (OpenID Connect)
     * 
     * Client sử dụng access_token cho các request tiếp theo:
     * Authorization: Bearer {access_token}
     * 
     * @param dto LoginRequestDto chứa username và password
     * @return ResponseEntity<TokenExchangeResponse> chứa JWT tokens (HTTP 200)
     * @throws FeignException nếu username/password không đúng (HTTP 401)
     * 
     * @apiNote POST /api/v1/public/login
     * @apiNote Content-Type: application/json
     * @apiNote No authentication required
     * 
     * @example
     *          Request:
     *          {
     *          "username": "johndoe",
     *          "password": "password123"
     *          }
     * 
     *          Response:
     *          {
     *          "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     *          "expires_in": "300",
     *          "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *          "token_type": "Bearer",
     *          "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     *          "scope": "openid profile email"
     *          }
     */
    @PostMapping("/login")
    ResponseEntity<TokenExchangeResponse> login(@RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }
}