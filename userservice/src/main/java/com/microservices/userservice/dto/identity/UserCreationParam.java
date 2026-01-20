package com.microservices.userservice.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho request tạo user trong Keycloak sử dụng Admin REST API.
 * 
 * Endpoint: POST /admin/realms/microservices/users
 * Yêu cầu: Bearer token có quyền admin
 * 
 * Keycloak sẽ:
 * - Tạo user với thông tin được cung cấp
 * - Hash và lưu password an toàn
 * - Trả về HTTP 201 với Location header chứa userId
 * 
 * Example request body:
 * {
 * "username": "johndoe",
 * "email": "john@example.com",
 * "firstName": "John",
 * "lastName": "Doe",
 * "enabled": true,
 * "emailVerified": false,
 * "credentials": [
 * {
 * "type": "password",
 * "value": "password123",
 * "temporary": false
 * }
 * ]
 * }
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationParam {

    /** Username dùng để đăng nhập - bắt buộc và phải unique */
    String username;

    /**
     * Trạng thái kích hoạt của user.
     * true: User có thể đăng nhập ngay
     * false: User bị disable, không thể đăng nhập
     */
    boolean enabled;

    /** Email của user - nên là unique */
    String email;

    /**
     * Trạng thái xác thực email.
     * true: Email đã được xác thực
     * false: Cần xác thực email (gửi email verification)
     */
    boolean emailVerified;

    /** Tên đệm của user */
    String firstName;

    /** Họ của user */
    String lastName;

    /**
     * Danh sách credentials (thường là password).
     * Keycloak sẽ hash password trước khi lưu.
     * 
     * Example:
     * [
     * {
     * "type": "password",
     * "value": "password123",
     * "temporary": false
     * }
     * ]
     */
    List<Credential> credentials;
}
