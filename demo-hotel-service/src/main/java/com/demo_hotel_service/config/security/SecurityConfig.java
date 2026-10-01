package com.demo_hotel_service.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.PostMapping;

import com.common.security.JwtFilter;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @PostMapping("/api/bookings/{generalBookingId}/units")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    new AntPathRequestMatcher("/api/bookings", "POST")
                ).permitAll()
                .requestMatchers(
                    new AntPathRequestMatcher("/api/bookings/**", "GET"),
                    new AntPathRequestMatcher("/api/bookings/**", "PUT"),
                    new AntPathRequestMatcher("/api/bookings/**", "DELETE"),
                    new AntPathRequestMatcher("/api/bookings/*/units", "POST")
                ).authenticated()
                .requestMatchers(
                    new AntPathRequestMatcher("/api/rooms", "POST"),
                    new AntPathRequestMatcher("/api/rooms", "PUT"),
                    new AntPathRequestMatcher("/api/rooms/*", "DELETE"),
                    new AntPathRequestMatcher("/api/spas", "POST"),
                    new AntPathRequestMatcher("/api/spas", "PUT"),
                    new AntPathRequestMatcher("/api/spas/*", "DELETE"),
                    new AntPathRequestMatcher("/api/image", "DELETE"),
                    new AntPathRequestMatcher("/api/admin/bookings")
                ).hasRole("ADMIN")
                .requestMatchers("/api/internal/bookings/claim").hasRole("SYSTEM_SERVICE")
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}