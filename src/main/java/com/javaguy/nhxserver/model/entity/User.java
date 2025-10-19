package com.javaguy.nhxserver.model.entity;

import com.javaguy.nhxserver.model.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "phone_number")
       })
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber ="";

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName="";

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "wallet_address")
    private String walletAddress;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
