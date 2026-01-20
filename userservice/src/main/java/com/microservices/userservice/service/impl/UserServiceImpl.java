package com.microservices.userservice.service.impl;

import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.microservices.userservice.dto.CreateUserRequestDTO;
import com.microservices.userservice.dto.LoginRequestDto;
import com.microservices.userservice.dto.UserResponseDTO;
import com.microservices.userservice.dto.identity.Credential;
import com.microservices.userservice.dto.identity.TokenExchangeParam;
import com.microservices.userservice.dto.identity.TokenExchangeResponse;
import com.microservices.userservice.dto.identity.UserCreationParam;
import com.microservices.userservice.dto.identity.UserTokenExchangeParam;
import com.microservices.userservice.entity.User;
import com.microservices.userservice.repository.IdentityClient;
import com.microservices.userservice.repository.UserRepository;
import com.microservices.userservice.service.IUserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của User Service - Quản lý người dùng và tích hợp Keycloak.
 * 
 * Service này xử lý:
 * - Đăng ký người dùng mới (tạo trong Keycloak và lưu local DB)
 * - Đăng nhập người dùng thông qua Keycloak OAuth2
 * - CRUD operations cho thông tin người dùng
 * - Tích hợp với Keycloak Identity Provider
 * 
 * @author User Service Team
 * @version 1.0
 * @since 2025-12-10
 */
@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    /** Repository để thao tác với database local */
    @Autowired
    private UserRepository userRepository;

    /** Feign client để gọi Keycloak REST APIs */
    @Autowired
    private IdentityClient identityClient;

    /** OAuth2 Client ID được cấu hình trong Keycloak */
    @Value("${idp.client-id}")
    @NonFinal
    String clientId;

    /** OAuth2 Client Secret để xác thực với Keycloak */
    @Value("${idp.client-secret}")
    @NonFinal
    String clientSecret;

    /**
     * Tạo người dùng mới trong hệ thống.
     * 
     * Luồng xử lý:
     * 1. Lấy client access token từ Keycloak (server-to-server authentication)
     * 2. Sử dụng token để tạo user trong Keycloak
     * 3. Extract userId từ response header Location
     * 4. Lưu thông tin user vào database local
     * 5. Trả về UserResponseDTO
     * 
     * @param dto CreateUserRequestDTO chứa thông tin user cần tạo
     * @return UserResponseDTO chứa thông tin user đã được tạo (không có password)
     * @throws RuntimeException nếu không thể tạo user trong Keycloak hoặc DB
     */
    @Override
    public UserResponseDTO createUser(CreateUserRequestDTO dto) {
        // Bước 1: Lấy client access token từ Keycloak sử dụng Client Credentials Grant
        // Token này cho phép service gọi Admin APIs của Keycloak
        var token = identityClient.exchangeClientToken(TokenExchangeParam.builder()
                .grant_type("client_credentials")
                .client_secret(clientSecret)
                .client_id(clientId)
                .scope("openid")
                .build());

        log.info("Token info", token);

        // Bước 2: Tạo user trong Keycloak sử dụng Admin REST API
        // Password được hash và lưu an toàn trong Keycloak
        var creationResponse = identityClient.createUser(UserCreationParam.builder()
                .username(dto.getUsername())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .enabled(true) // User có thể đăng nhập ngay
                .emailVerified(false) // Yêu cầu verify email sau
                .credentials(List.of(Credential.builder()
                        .type("password")
                        .temporary(false) // Password không phải tạm thời
                        .value(dto.getPassword())
                        .build()))
                .build(), "Bearer " + token.getAccessToken());

        // Bước 3: Extract userId từ Location header của response
        // Location header có dạng:
        // http://keycloak/admin/realms/microservices/users/{userId}
        String userId = extractUserId(creationResponse);
        log.info("UserId {}", userId);

        // Bước 4: Lưu thông tin user vào database local
        // userId từ Keycloak được lưu làm primary key
        User user = new User();
        user.setId(userId); // UUID từ Keycloak làm primary key
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFirstName() + " " + dto.getLastName()); // Ghép firstName và lastName
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAvatarUrl(dto.getAvatarUrl());

        user = userRepository.save(user);

        // Bước 5: Convert entity sang DTO và trả về (không bao gồm password)
        return toDTO(user);
    }

    /**
     * Lấy danh sách tất cả người dùng trong hệ thống.
     * 
     * @return List<UserResponseDTO> danh sách tất cả users
     */
    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin người dùng theo ID.
     * 
     * @param id ID của user (UUID từ Keycloak)
     * @return UserResponseDTO thông tin user
     * @throws RuntimeException nếu không tìm thấy user với ID này
     */
    @Override
    public UserResponseDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toDTO(user);
    }

    /**
     * Cập nhật thông tin người dùng.
     * 
     * Lưu ý: Chỉ cập nhật thông tin trong database local.
     * Để cập nhật thông tin trong Keycloak, cần gọi Keycloak Admin API riêng.
     * 
     * @param id  ID của user cần cập nhật (UUID từ Keycloak)
     * @param dto CreateUserRequestDTO chứa thông tin mới
     * @return UserResponseDTO thông tin user sau khi cập nhật
     * @throws RuntimeException nếu không tìm thấy user với ID này
     */
    @Override
    public UserResponseDTO updateUser(String id, CreateUserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Cập nhật các trường thông tin
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFirstName() + " " + dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAvatarUrl(dto.getAvatarUrl());

        return toDTO(userRepository.save(user));
    }

    /**
     * Xóa người dùng khỏi database local.
     * 
     * Lưu ý: Chỉ xóa khỏi database local, user vẫn tồn tại trong Keycloak.
     * Để xóa hoàn toàn, cần gọi Keycloak Admin API để xóa user trong Keycloak.
     * 
     * @param id ID của user cần xóa (UUID từ Keycloak)
     */
    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    /**
     * Chuyển đổi User entity sang UserResponseDTO.
     * 
     * DTO này không chứa thông tin nhạy cảm như password.
     * 
     * @param user User entity
     * @return UserResponseDTO
     */
    private UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId()) // UUID từ Keycloak
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * Extract userId từ Location header của Keycloak response.
     * 
     * Khi tạo user thành công, Keycloak trả về HTTP 201 với Location header:
     * Location: http://keycloak:8180/admin/realms/microservices/users/{userId}
     * 
     * Method này parse URL và lấy userId (phần cuối cùng của path).
     * 
     * @param response ResponseEntity từ Keycloak createUser API
     * @return String userId (UUID format)
     * @throws IllegalStateException nếu Location header không tồn tại
     */
    private String extractUserId(ResponseEntity<?> response) {
        List<String> locations = response.getHeaders().get("Location");
        if (locations == null || locations.isEmpty()) {
            throw new IllegalStateException("Location header is missing in the response");
        }

        // Parse URL: http://keycloak/admin/realms/microservices/users/{userId}
        String location = locations.get(0);
        String[] splitedStr = location.split("/");
        // Lấy phần cuối cùng - đó là userId
        return splitedStr[splitedStr.length - 1];
    }

    /**
     * Đăng nhập người dùng thông qua Keycloak OAuth2 Password Grant.
     * 
     * Luồng xử lý:
     * 1. Gửi username/password đến Keycloak Token Endpoint
     * 2. Keycloak validate credentials
     * 3. Nếu hợp lệ, Keycloak trả về JWT tokens:
     * - access_token: Sử dụng để gọi protected APIs (expire sau 5-15 phút)
     * - refresh_token: Sử dụng để lấy access_token mới (expire sau 30 phút)
     * - id_token: Chứa thông tin user (OpenID Connect)
     * 
     * @param dto LoginRequestDto chứa username và password
     * @return TokenExchangeResponse chứa access_token, refresh_token, id_token
     * @throws FeignException nếu credentials không đúng (401 Unauthorized)
     */
    @Override
    public TokenExchangeResponse login(LoginRequestDto dto) {
        // Gọi Keycloak Token Endpoint với OAuth2 Password Grant
        var token = identityClient.exchangeUserToken(UserTokenExchangeParam.builder()
                .grant_type("password") // OAuth2 Password Grant Type
                .client_id(clientId)
                .client_secret(clientSecret)
                .scope("openid") // OpenID Connect scope
                .username(dto.getUsername())
                .password(dto.getPassword())
                .build());
        return token;
    }
}
