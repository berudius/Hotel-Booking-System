package com.common.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {
    @Value("${app.security.jwt.secret-key}")
    private String jwtSecretKey;
    @Value("${app.security.jwt.expiration-ms:300000}")
    private long jwtExpirationMs;

    // Генерація токена для користувача (передається в мікросервіси після логіну)
    public String generateUserToken(Long userId, String login, String role, String phoneNumber){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("phoneNumber", phoneNumber);

        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));

        return Jwts.builder()
            .claims(claims)
            .subject(login)
            .issuer("edemium-api-gateway")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key)
            .compact();
    }

    // Генерація Сервісного токена (для виклику /api/auth-principal під час логіну)
    public String generateServiceToken(String serviceName) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));

        return Jwts.builder()
            .claim("role", "SYSTEM_SERVICE")
            .subject(serviceName)
            .issuer("edemium-api-gateway")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key)
            .compact();
    }

    public Claims validateAndGetClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
        Claims claims = Jwts.parser()
                .verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();

        return claims;
    }
         
}
