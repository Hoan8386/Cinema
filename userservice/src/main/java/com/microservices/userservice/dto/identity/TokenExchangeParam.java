package com.microservices.userservice.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO cho Client Credentials Token Exchange với Keycloak.
 * 
 * Sử dụng trong OAuth2 Client Credentials Grant Flow.
 * Flow này dùng cho server-to-server authentication, cho phép
 * service lấy access token để gọi Keycloak Admin APIs.
 * 
 * Endpoint: POST /realms/microservices/protocol/openid-connect/token
 * Content-Type: application/x-www-form-urlencoded
 * 
 * Example request:
 * grant_type=client_credentials
 * &client_id=microservices-client
 * &client_secret=your-secret
 * &scope=openid
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenExchangeParam {

    /**
     * OAuth2 grant type - luôn là "client_credentials" cho server-to-server auth.
     * Grant type này không yêu cầu user credentials.
     */
    String grant_type;

    /** OAuth2 Client ID đã đăng ký trong Keycloak */
    String client_id;

    /** OAuth2 Client Secret - dùng để xác thực client */
    String client_secret;

    /**
     * OAuth2 scope - thường là "openid" cho OpenID Connect.
     * Có thể thêm các scopes khác: "profile", "email", "roles"
     */
    String scope;
}
