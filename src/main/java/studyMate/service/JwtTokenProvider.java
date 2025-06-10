package studyMate.service;

import jakarta.annotation.PostConstruct;
import studyMate.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    @PostConstruct
    public void init() {
        log.info("JWT Properties loaded - Secret Key length: {}", 
                jwtProperties.getSecretKey() != null ? jwtProperties.getSecretKey().length() : "null");
        log.info("Access Token Validity: {} seconds", jwtProperties.getAccessTokenValidityInSeconds());
        log.info("Refresh Token Validity: {} seconds", jwtProperties.getRefreshTokenValidityInSeconds());
        
        if (jwtProperties.getSecretKey() == null) {
            throw new IllegalStateException("JWT secret key is not configured properly");
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getRefreshTokenValidityInSeconds() * 1000);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        String email = getUsername(token);
        return new UsernamePasswordAuthenticationToken(email, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationTime() {
        return jwtProperties.getAccessTokenValidityInSeconds();
    }
} 