package com.microservices.userservice.dto.identity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho response từ Keycloak Token Endpoint.
 * 
 * Được trả về từ cả 2 flows:
 * - Client Credentials Grant (exchangeClientToken)
 * - Password Grant (exchangeUserToken - login)
 * 
 * JWT Tokens structure:
 * - access_token: JWT token dùng để gọi protected APIs
 * Format: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
 * Chứa: user info, roles, permissions, expiration time
 * 
 * - refresh_token: Token dùng để lấy access_token mới khi hết hạn
 * Có thời gian sống dài hơn access_token
 * 
 * - id_token: OpenID Connect token chứa thông tin user
 * Chứa: sub (user ID), name, email, preferred_username, etc.
 * 
 * Client sử dụng access_token cho các request:
 * Authorization: Bearer {access_token}
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TokenExchangeResponse {

    /**
     * JWT Access Token để gọi protected APIs.
     * Expire sau 5-15 phút (configurable trong Keycloak).
     * Format: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     */
    String accessToken;

    /**
     * Thời gian sống của access_token (giây).
     * Ví dụ: "300" = 5 phút
     */
    String expiresIn;

    /**
     * Thời gian sống của refresh_token (giây).
     * Ví dụ: "1800" = 30 phút
     */
    String refreshExpiresIn;

    /**
     * Loại token - luôn là "Bearer" trong OAuth2.
     * Client phải gửi: Authorization: Bearer {access_token}
     */
    String tokenType;

    /**
     * OpenID Connect ID Token chứa thông tin user.
     * Chỉ có trong Password Grant flow.
     * Null trong Client Credentials Grant.
     */
    String idToken;

    /**
     * OAuth2 scopes được granted.
     * Ví dụ: "openid profile email"
     */
    String scope;
}
