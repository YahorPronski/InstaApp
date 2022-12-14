package com.company.authserver.service.impl;

import com.company.authserver.dto.LoginRequest;
import com.company.authserver.dto.LoginResponse;
import com.company.authserver.dto.RegisterRequest;
import com.company.authserver.service.AuthService;
import com.company.authserver.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse loginUser(LoginRequest loginRequest) {
        Long userId = webClientBuilder.build().post()
                .uri("http://user-service/api/users/id")
                .body(BodyInserters.fromValue(loginRequest))
                .retrieve()
                .bodyToMono(Long.class)
                .doOnError(error -> {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, error.getMessage());
                })
                .block();
        return generateTokens(userId.toString());
    }

    @Override
    public void registerUser(RegisterRequest registerRequest) {
        webClientBuilder.build().post()
                .uri("http://user-service/api/users")
                .body(BodyInserters.fromValue(registerRequest))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, error.getMessage());
                })
                .block();
    }

    @Override
    public Optional<Long> getUserIdFromToken(String token) {
        if (jwtUtil.validateAccessToken(token)) {
            String userId = jwtUtil.getAccessTokenClaims(token).getSubject();
            return Optional.of(Long.parseLong(userId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<LoginResponse> refreshToken(String token) {
        if (jwtUtil.validateRefreshToken(token)) {
            String userId = jwtUtil.getRefreshTokenClaims(token).getSubject();
            return Optional.of(generateTokens(userId));
        }
        return Optional.empty();
    }

    public LoginResponse generateTokens(String userId) {
        return LoginResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userId))
                .refreshToken(jwtUtil.generateRefreshToken(userId))
                .build();
    }
}
