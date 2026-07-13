package com.enterpriseai.backend.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterpriseai.backend.dto.AuthResponse;
import com.enterpriseai.backend.entity.RefreshToken;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.UnauthorizedException;
import com.enterpriseai.backend.repository.RefreshTokenRepository;
import com.enterpriseai.backend.security.JwtService;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            JwtService jwtService) {

        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(generateSecureToken());
        refreshToken.setExpiresAt(
                LocalDateTime.now().plus(refreshExpiration, ChronoUnit.MILLIS));
        refreshToken.setRevoked(false);

        return repository.save(refreshToken);
    }

    @Transactional
    public AuthResponse rotate(String token) {
        RefreshToken currentToken = getValidToken(token);
        User user = currentToken.getUser();

        currentToken.setRevoked(true);
        repository.save(currentToken);

        RefreshToken newRefreshToken = createRefreshToken(user);
        String accessToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(accessToken, newRefreshToken.getToken());
    }

    @Transactional
    public void revoke(String token) {
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        repository.save(refreshToken);
    }

    private RefreshToken getValidToken(String token) {
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            repository.save(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        return refreshToken;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
