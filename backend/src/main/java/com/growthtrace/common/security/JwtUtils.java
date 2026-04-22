package com.growthtrace.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${growthtrace.security.jwt.secret}")
    private String secret;

    @Value("${growthtrace.security.jwt.issuer}")
    private String issuer;

    @Value("${growthtrace.security.jwt.access-token-ttl-minutes:120}")
    private long accessTokenTtlMinutes;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.length() < 32) {
            log.warn("growthtrace.security.jwt.secret 长度建议 ≥32 字符，当前配置可能不够安全");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes.length >= 32 ? keyBytes : padTo32(keyBytes));
    }

    private byte[] padTo32(byte[] raw) {
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 32));
        return padded;
    }

    public String generateAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(accessTokenTtlMinutes));
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("uname", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        Object v = claims.get("uname");
        return v == null ? null : v.toString();
    }
}
