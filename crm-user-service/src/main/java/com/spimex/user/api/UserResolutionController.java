package com.spimex.user.api;

import com.spimex.user.api.dto.UserResolutionRequest;
import com.spimex.user.api.dto.UserResolutionResponse;
import com.spimex.user.application.UserResolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/internal/v1/user-resolutions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserResolutionController {

    private final UserResolutionService userResolutionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResolutionResponse resolve(@Valid @RequestBody UserResolutionRequest request) {
        return UserResolutionResponse.from(userResolutionService.resolve(request.login(), request.email()));
    }
}
