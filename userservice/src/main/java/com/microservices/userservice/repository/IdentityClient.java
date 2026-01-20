package com.microservices.userservice.repository;

import feign.Body;
import feign.QueryMap;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.microservices.userservice.dto.identity.TokenExchangeParam;
import com.microservices.userservice.dto.identity.TokenExchangeResponse;
import com.microservices.userservice.dto.identity.UserCreationParam;
import com.microservices.userservice.dto.identity.UserTokenExchangeParam;

/**
 * Feign Client để tích hợp với Keycloak Identity Provider.
 * 
 * Interface này định nghĩa các REST API calls đến Keycloak server:
 * - Token Exchange (lấy access token)
 * - User Management (tạo user trong Keycloak)
 * - Authentication (đăng nhập user)
 * 
 * Keycloak URL được cấu hình trong application.yml: ${idp.url}
 * 
 * Feign Client tự động:
 * - Xử lý serialization/deserialization JSON
 * - Retry logic và error handling
 * - Load balancing (nếu có multiple Keycloak instances)
 * 
 * @author User Service Team
 * @version 1.0
 */
@FeignClient(name = "identity-client", url = "${idp.url}")
public interface IdentityClient {

    /**
     * Lấy client access token từ Keycloak sử dụng Client Credentials Grant.
     * 
     * Đây là server-to-server authentication flow, cho phép service gọi
     * Keycloak Admin APIs để tạo user, quản lý roles, etc.
     * 
     * OAuth2 Flow: Client Credentials Grant
     * - Grant Type: client_credentials
     * - Scope: openid
     * 
     * @param param TokenExchangeParam chứa client_id, client_secret, grant_type
     * @return TokenExchangeResponse chứa access_token và metadata
     * 
     * @apiNote POST /realms/microservices/protocol/openid-connect/token
     * @apiNote Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping(value = "/realms/microservices/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    TokenExchangeResponse exchangeClientToken(@QueryMap() TokenExchangeParam param);

    /**
     * Tạo user mới trong Keycloak sử dụng Admin REST API.
     * 
     * API này yêu cầu Bearer token có quyền admin để gọi.
     * Token được lấy từ exchangeClientToken() method.
     * 
     * Response trả về HTTP 201 Created với Location header chứa userId:
     * Location: http://keycloak/admin/realms/microservices/users/{userId}
     * 
     * @param body  UserCreationParam chứa thông tin user (username, email,
     *              password, ...)
     * @param token Bearer token có quyền admin (format: "Bearer {access_token}")
     * @return ResponseEntity với Location header chứa URL của user mới tạo
     * 
     * @apiNote POST /admin/realms/microservices/users
     * @apiNote Content-Type: application/json
     * @apiNote Headers: Authorization: Bearer {admin_token}
     */
    @PostMapping(value = "admin/realms/microservices/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createUser(@RequestBody() UserCreationParam body,
            @RequestHeader("authorization") String token);

    /**
     * Đăng nhập user và lấy JWT tokens sử dụng OAuth2 Password Grant.
     * 
     * Đây là user authentication flow, cho phép user đăng nhập bằng
     * username/password và nhận JWT tokens để gọi protected APIs.
     * 
     * OAuth2 Flow: Password Grant (Resource Owner Password Credentials)
     * - Grant Type: password
     * - Scope: openid profile email
     * 
     * Response chứa:
     * - access_token: Dùng để gọi protected APIs
     * - refresh_token: Dùng để lấy access_token mới khi hết hạn
     * - id_token: Chứa thông tin user (OpenID Connect)
     * 
     * @param param UserTokenExchangeParam chứa username, password, client
     *              credentials
     * @return TokenExchangeResponse chứa access_token, refresh_token, id_token
     * @throws FeignException với status 401 nếu credentials không đúng
     * 
     * @apiNote POST /realms/microservices/protocol/openid-connect/token
     * @apiNote Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping(value = "realms/microservices/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    TokenExchangeResponse exchangeUserToken(@QueryMap() UserTokenExchangeParam param);

}
