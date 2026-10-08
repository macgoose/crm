package com.spimex.user.application;

import com.spimex.user.client.dto.UserResponse;
import com.spimex.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserQueryService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<UserResponse> findById(UUID userId) {
        return userRepository
            .findById(userId)
            .map(user -> new UserResponse(
                user.getId(), user.getLogin(), user.getEmail(),
                user.getFio(), user.getShortName(), user.isActive(),
                user.getVersion()
            ));
    }
}
