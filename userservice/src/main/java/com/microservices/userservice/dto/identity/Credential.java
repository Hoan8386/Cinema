package com.microservices.userservice.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho credential (thông tin xác thực) của user trong Keycloak.
 * 
 * Được sử dụng trong UserCreationParam khi tạo user mới.
 * Keycloak hỗ trợ nhiều loại credentials: password, otp, totp, etc.
 * 
 * Trong hệ thống này, chủ yếu sử dụng type="password".
 * 
 * Keycloak sẽ tự động:
 * - Hash password với thuật toán mạnh (bcrypt/pbkdf2)
 * - Lưu trữ hash trong database của Keycloak
 * - Không bao giờ lưu plain text password
 * 
 * @author User Service Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credential {

    /**
     * Loại credential.
     * Các giá trị phổ biến:
     * - "password": Password-based authentication
     * - "otp": One-time password
     * - "totp": Time-based one-time password (Google Authenticator)
     */
    String type;

    /**
     * Giá trị của credential.
     * Đối với type="password": plain text password sẽ được hash bởi Keycloak.
     * Ví dụ: "password123"
     */
    String value;

    /**
     * Password có phải là tạm thời không.
     * true: User bắt buộc phải đổi password khi đăng nhập lần đầu
     * false: Password vĩnh viễn, không cần đổi
     */
    boolean temporary;
}
