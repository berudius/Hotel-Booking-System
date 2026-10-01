package com.demo_user_service.data.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.demo_user_service.data.enums.AccountState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, unique = true, length = 32)
    private String login;
    @Column(nullable = false)
    private String role;
    @Column(nullable = false)
    private String hashPassword;
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private AccountState accountState = AccountState.ACTIVE;

    // int trustLevel;
    // int consecutiveCancellations;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registered;

}
