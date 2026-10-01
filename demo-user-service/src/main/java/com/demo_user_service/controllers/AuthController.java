package com.demo_user_service.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

import com.common.security.AuthPrincipal;
import com.common.security.AuthPrincipalDto;
import com.common.validation.OnCreate;
import com.demo_user_service.data.dto.UserDto;
import com.demo_user_service.services.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/request-registration-otp")
    public ResponseEntity<Void> requestRegistrationOtp(
            @RequestParam @NotBlank String login, 
            @RequestParam @NotBlank String phoneNumber) {
        authService.requestRegistrationOtp(login, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign_up")
    public ResponseEntity<Void> signUp(
            @ModelAttribute @Validated(OnCreate.class) UserDto userDto, 
            @RequestParam String otpCode) {
        
        authService.verifyAndAddUser(userDto, otpCode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/internal/auth-principal")
    public ResponseEntity<AuthPrincipalDto> authPrincipal(@RequestParam @NotBlank String login) {
        return ResponseEntity.ok(authService.loadUserByUsername(login));
    }

    @PostMapping("/users/profile/phone/request-update")
    public ResponseEntity<Map<String, String>> requestPhoneUpdate(
            @RequestParam @NotBlank String newPhone,
            @AuthenticationPrincipal AuthPrincipal principal) {
        
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        authService.requestPhoneVerification(newPhone);
        return ResponseEntity.ok(Map.of("message", "Код підтвердження відправлено на номер " + newPhone));
    }

    @PostMapping("/users/profile/phone/verify-update")
    public ResponseEntity<Map<String, String>> verifyPhoneUpdate(
            @RequestParam @NotBlank String newPhone,
            @RequestParam String otpCode,
            @AuthenticationPrincipal AuthPrincipal principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        authService.verifyAndChangePhone(principal.getId(), newPhone, otpCode);
        return ResponseEntity.ok(Map.of("message", "Номер телефону успішно оновлено! Ваші бронювання синхронізовано."));
    }

    @PostMapping("/request-reset-otp")
    public ResponseEntity<Void> requestResetOtp(@RequestParam @NotBlank String phoneNumber) {
        authService.requestResetOtp(phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam @NotBlank String phoneNumber, 
            @RequestParam String otpCode, 
            @RequestParam @NotBlank String newPassword) {
        authService.resetPassword(phoneNumber, otpCode, newPassword);
        return ResponseEntity.ok(Map.of("message", "Пароль успішно змінено!"));
    }

    @GetMapping("/internal/users/exists-by-phone")
    public ResponseEntity<Boolean> checkPhoneExists(@RequestParam @NotBlank String phoneNumber) {
        return ResponseEntity.ok(authService.existsByPhoneNumber(phoneNumber)); 
    }
}