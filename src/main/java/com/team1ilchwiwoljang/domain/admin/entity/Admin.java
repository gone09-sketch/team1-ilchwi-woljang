package com.team1ilchwiwoljang.domain.admin.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "admins")
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column
    private LocalDateTime deletedAt;

    public static Admin create(String email, String encodedPassword, String name, String phone) {
        Admin admin = new Admin();
        admin.email = email;
        admin.password = encodedPassword;
        admin.name = name;
        admin.phone = phone;
        return admin;
    }
}
