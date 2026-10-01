package com.api.gateway.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.common.security.AuthPrincipal;
import com.common.security.AuthPrincipalDto;
import com.common.security.JwtUtil;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final RestClient restClient;
    private final JwtUtil jwtUtil;

    public CustomUserDetailsService(@Value("${user_service_url}") String userServiceUrl, JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String login) {

            String jwtServiceToken = jwtUtil.generateServiceToken("api-gateway");

            ResponseSpec responseSpec = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/auth-principal")
                            .queryParam("login", login)
                            .build())
                    .header("Authorization", "Bearer " + jwtServiceToken)
                    .retrieve();

            AuthPrincipalDto authPrincipalDto = responseSpec.body(AuthPrincipalDto.class);
            AuthPrincipal authPrincipal = new AuthPrincipal(authPrincipalDto);

            return authPrincipal;
    }
}
