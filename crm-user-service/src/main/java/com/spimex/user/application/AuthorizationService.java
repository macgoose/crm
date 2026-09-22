package com.spimex.user.application;

import com.spimex.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final UserRepository userRepository;
    private final UserResolutionService userResolutionService;

    @Transactional(readOnly = true)
    public AuthorizationDecision authorize(String login, String email, String permission) {
        UserResolution resolution = userResolutionService.resolve(login, email);
        if (!resolution.resolved()) {
            log.warn("Authorization denied during identity resolution: permission={} reason={} userId={}",
                permission, resolution.denialReason(), resolution.userId());
            return resolution.userId() == null
                ? AuthorizationDecision.denied(resolution.denialReason())
                : AuthorizationDecision.denied(
                    resolution.denialReason(), resolution.userId(), resolution.userVersion());
        }

        boolean granted = userRepository.hasPermission(resolution.userId(), normalizeCode(permission));
        if (!granted) {
            log.info("Authorization denied: userId={} permission={} reason={}",
                resolution.userId(), normalizeCode(permission), DenialReason.PERMISSION_NOT_GRANTED);
            return AuthorizationDecision.denied(
                DenialReason.PERMISSION_NOT_GRANTED,
                resolution.userId(),
                resolution.userVersion());
        }

        log.info("Authorization allowed: userId={} permission={}",
            resolution.userId(), normalizeCode(permission));
        return AuthorizationDecision.allowed(resolution.userId(), resolution.userVersion());
    }

    private static String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

}
