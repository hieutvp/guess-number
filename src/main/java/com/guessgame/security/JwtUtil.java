package com.guessgame.security;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import io.jsonwebtoken.security.Keys;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        // Initialize the secret key and expiration time
        secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String username) {
        // Generate JWT token using the secret key and expiration time
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, String username) {
        // Validate the JWT token
        String user = extractUsername(token);
    }

    public String extractUsername(String token) {
        // Extract the username from the JWT token
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenExpired(String token) {
        // Check if the JWT token is expired
        Date expirationDate = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expirationDate.before(new Date());
    }

}
