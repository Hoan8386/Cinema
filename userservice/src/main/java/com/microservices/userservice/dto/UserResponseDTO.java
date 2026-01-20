package com.microservices.userservice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho response trả về thông tin user.
 * 
 * DTO này KHÔNG chứa thông tin nhạy cảm như password.
 * Được sử dụng trong tất cả API responses liên quan đến user.
 * 
 * Sử dụng trong:
 * - GET /api/v1/users - Lấy danh sách users
 * - GET /api/v1/users/{id} - Lấy chi tiết user
 * - POST /api/v1/users - Response sau khi tạo user
 * - PUT /api/v1/users/{id} - Response sau khi cập nhật user
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@Builder
public class UserResponseDTO {

    /** ID của user (UUID từ Keycloak) - đây là primary key */
    private String id;

    /** Email của user */
    private String email;

    /** Họ và tên đầy đủ của user */
    private String fullName;

    /** Số điện thoại của user */
    private String phoneNumber;

    /** URL avatar của user */
    private String avatarUrl;
}
