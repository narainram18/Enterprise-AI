package com.enterpriseai.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        JwtValidationResult result = validateToken(token);

        return result.isValid() ? result.getUsername() : null;
    }

    public boolean isTokenValid(String token, String email) {
        JwtValidationResult result = validateToken(token);

        return result.isValid() && email.equals(result.getUsername());
    }

    public JwtValidationResult validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            String username = claims.getSubject();

            if (username == null || username.isBlank()) {
                return JwtValidationResult.invalid("Invalid token");
            }

            if (isTokenExpired(claims)) {
                return JwtValidationResult.invalid("JWT token has expired");
            }

            return JwtValidationResult.valid(username);
        } catch (ExpiredJwtException ex) {
            return JwtValidationResult.invalid("JWT token has expired");
        } catch (SignatureException ex) {
            return JwtValidationResult.invalid("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            return JwtValidationResult.invalid("Malformed JWT token");
        } catch (UnsupportedJwtException ex) {
            return JwtValidationResult.invalid("Unsupported JWT token");
        } catch (JwtException | IllegalArgumentException ex) {
            return JwtValidationResult.invalid("Invalid JWT token");
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public static class JwtValidationResult {

        private final boolean valid;
        private final String username;
        private final String errorMessage;

        private JwtValidationResult(
                boolean valid,
                String username,
                String errorMessage) {

            this.valid = valid;
            this.username = username;
            this.errorMessage = errorMessage;
        }

        static JwtValidationResult valid(String username) {
            return new JwtValidationResult(true, username, null);
        }

        static JwtValidationResult invalid(String errorMessage) {
            return new JwtValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getUsername() {
            return username;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
