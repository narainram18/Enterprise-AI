package com.enterpriseai.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.dto.AuthResponse;
import com.enterpriseai.backend.dto.LoginRequest;
import com.enterpriseai.backend.dto.RegisterRequest;
import com.enterpriseai.backend.dto.UserProfileResponse;
import com.enterpriseai.backend.entity.RefreshToken;
import com.enterpriseai.backend.entity.Role;
import com.enterpriseai.backend.entity.User;
import com.enterpriseai.backend.exception.DuplicateResourceException;
import com.enterpriseai.backend.exception.ResourceNotFoundException;
import com.enterpriseai.backend.exception.UnauthorizedException;
import com.enterpriseai.backend.mapper.UserMapper;
import com.enterpriseai.backend.repository.UserRepository;
import com.enterpriseai.backend.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    public void register(RegisterRequest request) {

        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        repository.save(user);
        log.info("User registration succeeded email={}", request.getEmail());
    }

    public AuthResponse login(LoginRequest request) {

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed reason=unknown_user email={}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed reason=invalid_password email={}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login succeeded email={}", user.getEmail());

        return new AuthResponse(token, refreshToken.getToken());
    }

    public UserProfileResponse getCurrentUser(String email) {

        User user = repository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return userMapper.toProfileResponse(user);
    }
}
