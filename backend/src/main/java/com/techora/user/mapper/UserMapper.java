package com.techora.user.mapper;

import com.techora.user.dto.UserSummary;
import com.techora.user.dto.UserSnapshot;
import com.techora.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserSummary toSummary(UserEntity user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    public UserSnapshot toSnapshot(UserEntity user) {
        return new UserSnapshot(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
