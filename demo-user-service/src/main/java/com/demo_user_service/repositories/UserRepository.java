package com.demo_user_service.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo_user_service.data.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByLogin(String login);
    public boolean existsByLogin(String login);
    public boolean existsByPhoneNumber(String newPhone);
    public Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByLoginOrPhoneNumber(String login, String phoneNumber);
}
