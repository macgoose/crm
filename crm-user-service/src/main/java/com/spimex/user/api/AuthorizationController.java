package com.spimex.user.api;

import com.spimex.user.api.dto.AuthorizationRequest;
import com.spimex.user.api.dto.AuthorizationResponse;
import com.spimex.user.application.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/internal/v1/authorization-decisions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthorizationResponse authorize(@Valid @RequestBody AuthorizationRequest request) {
        return AuthorizationResponse.from(
            authorizationService.authorize(request.login(), request.email(), request.permission())
        );
    }
}
