package com.common.security;

import java.io.IOException;

import javax.crypto.SecretKey;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${app.security.jwt.secret-key}")
    private String jwtSecretKey;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")){
            String token = header.substring(7);

            try{
                SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
                Claims claims = Jwts.parser()
                    .verifyWith(secretKey).build()
                    .parseSignedClaims(token)
                    .getPayload();

                String role = claims.get("role", String.class);

                if("SYSTEM_SERVICE".equals(role)){
                    authenticateAsService(claims);
                } else {
                    authenticateAsUser(claims);
                }

            } catch(Exception ex){
                ex.printStackTrace();
                throw new RuntimeException("Invalid JWT token: " + ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateAsService(Claims claims){
        String serviceName = claims.getSubject();
        String role = claims.get("role", String.class);
        
        var authorities = List.of(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(serviceName, null,  authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsUser(Claims claims){
        String login = claims.getSubject();
        String role = claims.get("role", String.class);
        String phoneNumber = claims.get("phoneNumber", String.class);

        Number userIdNumber = claims.get("userId", Number.class);
        Long userId = userIdNumber.longValue();

        AuthPrincipal authPrincipal = new AuthPrincipal();
        authPrincipal.setId(userId);
        authPrincipal.setLogin(login);
        authPrincipal.setRole(role);
        authPrincipal.setPhoneNumber(phoneNumber);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
