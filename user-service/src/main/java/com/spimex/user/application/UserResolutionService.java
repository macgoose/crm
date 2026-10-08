package com.spimex.user.application;

import com.spimex.user.domain.model.user.User;
import com.spimex.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserResolutionService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResolution resolve(String login, String email) {
        log.debug("Resolving user: loginPresent={} emailPresent={}", hasText(login), hasText(email));
        Optional<User> byLogin = findByLogin(login);
        Optional<User> byEmail = findByEmail(email);

        if (hasText(login) && hasText(email)
            && (byLogin.isEmpty() || byEmail.isEmpty()
            || !byLogin.get().getId().equals(byEmail.get().getId()))) {
            log.warn("User resolution denied: reason={} loginMatched={} emailMatched={}",
                DenialReason.IDENTITY_CONFLICT, byLogin.isPresent(), byEmail.isPresent());
            return UserResolution.denied(DenialReason.IDENTITY_CONFLICT);
        }

        Optional<User> resolved = byLogin.or(() -> byEmail);
        if (resolved.isEmpty()) {
            log.info("User resolution denied: reason={}", DenialReason.USER_NOT_FOUND);
            return UserResolution.denied(DenialReason.USER_NOT_FOUND);
        }

        User user = resolved.get();
        if (!user.isActive()) {
            log.info("User resolution denied: reason={} userId={}", DenialReason.USER_INACTIVE, user.getId());
            return UserResolution.denied(
                DenialReason.USER_INACTIVE, user.getId(), user.getVersion());
        }

        log.debug("User resolved: userId={} version={}", user.getId(), user.getVersion());
        return UserResolution.resolved(user.getId(), user.getVersion());
    }

    private Optional<User> findByLogin(String login) {
        return hasText(login) ? userRepository.findByLogin(normalize(login)) : Optional.empty();
    }

    private Optional<User> findByEmail(String email) {
        return hasText(email) ? userRepository.findByEmail(normalize(email)) : Optional.empty();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
