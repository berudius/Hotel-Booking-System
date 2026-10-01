package com.demo_user_service.services;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.security.AuthPrincipalDto;
import com.demo_user_service.data.dto.UserDto;
import com.demo_user_service.data.dto.dto_mappers.UserMapper;
import com.demo_user_service.data.enums.AccountState;
import com.demo_user_service.data.models.User;
import com.demo_user_service.exceptions.LoginAlreadyInUseException;
import com.demo_user_service.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final BookingIntegrationService bookingIntegrationService;

    public AuthPrincipalDto loadUserByUsername(String identifier) {
        // identifier може містити як логін, так і номер телефону
        User user = userRepository.findByLoginOrPhoneNumber(identifier, identifier)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Користувача з таким логіном або номером не знайдено"));

        AuthPrincipalDto authPrincipal = new AuthPrincipalDto();
        authPrincipal.setId(user.getId());
        authPrincipal.setLogin(user.getLogin());
        authPrincipal.setPhoneNumber(user.getPhoneNumber());
        authPrincipal.setPassword(user.getHashPassword());
        authPrincipal.setRole(user.getRole());
        authPrincipal.setIsEnabled(true);
        authPrincipal.setIsAccountNonLocked(user.getAccountState() != AccountState.BLOCKED);

        return authPrincipal;
    }

    public void requestPhoneVerification(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Цей номер телефону вже зареєстрований.");
        }
        otpService.generateAndSendOtp(phoneNumber);
    }

    public void requestRegistrationOtp(String login, String phoneNumber) {
        if (userRepository.existsByLogin(login)) {
            throw new LoginAlreadyInUseException("Цей логін вже використовується.");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Цей номер телефону вже зареєстрований.");
        }
        otpService.generateAndSendOtp(phoneNumber);
    }

    @Transactional
    public void verifyAndAddUser(UserDto dto, String otpCode) {
        if (!otpService.validateOtp(dto.getPhoneNumber(), otpCode)) {
            throw new IllegalArgumentException("Неправильний або прострочений код підтвердження.");
        }
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new LoginAlreadyInUseException("Логін вже використовується");
        }

        User savedUser = userMapper.toEntity(dto);
        savedUser.setHashPassword(passwordEncoder.encode(dto.getPassword()));
        savedUser.setRole("USER");
        savedUser.setAccountState(AccountState.ACTIVE);

        userRepository.save(savedUser);
        bookingIntegrationService.claimBookingsForUser(savedUser.getId(), savedUser.getPhoneNumber());
    }

    @Transactional
    public void verifyAndChangePhone(Long userId, String newPhone, String otpCode) {
        if (!otpService.validateOtp(newPhone, otpCode)) {
            throw new IllegalArgumentException("Неправильний або прострочений код підтвердження.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено."));

        user.setPhoneNumber(newPhone);
        userRepository.save(user);

        bookingIntegrationService.claimBookingsForUser(user.getId(), user.getPhoneNumber());
    }

    public void requestResetOtp(String phoneNumber) {
        if (!userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Користувача з таким номером телефону не знайдено.");
        }
        otpService.generateAndSendOtp(phoneNumber);
    }

    @Transactional
    public void resetPassword(String phoneNumber, String otpCode, String newPassword) {
        if (!otpService.validateOtp(phoneNumber, otpCode)) {
            throw new IllegalArgumentException("Неправильний або прострочений код підтвердження.");
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено."));

        user.setHashPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
}