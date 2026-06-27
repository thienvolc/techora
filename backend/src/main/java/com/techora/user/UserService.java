package com.techora.user;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.user.dto.UserSummary;
import com.techora.user.dto.UserSnapshot;
import com.techora.user.entity.UserEntity;
import com.techora.user.entity.UserRole;
import com.techora.user.mapper.UserMapper;
import com.techora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserEntity createUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
        return createUser(username, rawPassword, UserRole.USER);
    }

    @Transactional
    public UserEntity createUser(String username, String rawPassword, UserRole role) {
        return userRepository.save(UserEntity.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toSummary)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserSnapshot getSnapshotOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toSnapshot)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserEntity getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }
}
