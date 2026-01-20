package com.microservices.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    // ĐÂY LÀ KHÓA CHÍNH - Map đúng với ID bên Keycloak
    @Id
    @Column(name = "id")
    private String id; // Lưu UUID từ Keycloak (Ví dụ: f546fe5e-2804...)

    // Email có thể lưu để dễ query/gửi mail (dù Keycloak cũng có)
    @Column(name = "email", unique = true)
    private String email;

    // Thông tin cá nhân (Keycloak ít quan tâm, Database bạn phải giữ)
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

}