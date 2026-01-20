package com.microservices.userservice.dto.identity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO cho User Password Token Exchange với Keycloak.
 * 
 * Sử dụng trong OAuth2 Password Grant Flow (Resource Owner Password
 * Credentials).
 * Flow này cho phép user đăng nhập bằng username/password và nhận JWT tokens.
 * 
 * Endpoint: POST /realms/microservices/protocol/openid-connect/token
 * Content-Type: application/x-www-form-urlencoded
 * 
 * Example request:
 * grant_type=password
 * &client_id=microservices-client
 * &client_secret=your-secret
 * &scope=openid
 * &username=johndoe
 * &password=password123
 * 
 * Response chứa:
 * - access_token: JWT token để gọi protected APIs
 * - refresh_token: Token để lấy access_token mới
 * - id_token: OpenID Connect ID token chứa user info
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserTokenExchangeParam {

    /**
     * OAuth2 grant type - luôn là "password" cho user authentication.
     * Grant type này yêu cầu username và password của user.
     */
    String grant_type;

    /** OAuth2 Client ID đã đăng ký trong Keycloak */
    String client_id;

    /** OAuth2 Client Secret - dùng để xác thực client */
    String client_secret;

    /**
     * OAuth2 scope - thường là "openid" cho OpenID Connect.
     * Có thể thêm: "profile", "email", "roles" để lấy thêm thông tin user
     */
    String scope;

    /** Username của user đăng nhập */
    String username;

    /** Password của user - sẽ được validate bởi Keycloak */
    String password;
}
