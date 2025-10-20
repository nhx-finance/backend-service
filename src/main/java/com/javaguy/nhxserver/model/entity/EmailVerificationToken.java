package com.javaguy.nhxserver.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    private LocalDateTime verifiedDate;
    
    @Column(nullable = false)
    private boolean used = false;
    
    public EmailVerificationToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.createdDate = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.used = false;
    }
    
    public EmailVerificationToken(String token, User user, int expiryTimeInMinutes) {
        this.token = token;
        this.user = user;
        this.createdDate = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusMinutes(expiryTimeInMinutes);
        this.used = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
    
    public void markAsUsed() {
        this.used = true;
        this.verifiedDate = LocalDateTime.now();
    }
}
