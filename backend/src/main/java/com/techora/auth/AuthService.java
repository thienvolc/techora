package com.techora.auth;

import com.techora.auth.dto.request.LoginRequest;
import com.techora.auth.dto.request.RegisterRequest;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.auth.infra.service.JwtService;
import com.techora.user.dto.UserSnapshot;
import com.techora.user.entity.UserEntity;
import com.techora.user.repository.UserRepository;
import com.techora.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
        UserSnapshot user = userService.createUser(request.username(), request.password());
        return jwtService.generateToken(
                user.id(),
                user.username(),
                user.role().name()
        );
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ResponseCode.BAD_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
        return jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
