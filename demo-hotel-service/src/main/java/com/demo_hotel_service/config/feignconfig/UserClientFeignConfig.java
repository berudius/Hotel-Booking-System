package com.demo_hotel_service.config.feignconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.common.security.JwtUtil;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class UserClientFeignConfig {
    private final JwtUtil jwtUtil;

    @Bean
    public RequestInterceptor userClientRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Генеруємо сервісний токен із роллю SYSTEM_SERVICE
                String serviceToken = jwtUtil.generateServiceToken("hotel-service");
                
                // Додаємо токен у заголовок Authorization
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken);
            }
        };
    }
}
