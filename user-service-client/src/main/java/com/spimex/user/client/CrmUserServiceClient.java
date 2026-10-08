package com.spimex.user.client;

import com.spimex.user.client.dto.*;

import java.util.UUID;

public interface CrmUserServiceClient {

    UserResponse getUser(UUID userId);

    AuthorizationResponse authorize(AuthorizationRequest request);

    UserResolutionResponse resolveUser(UserResolutionRequest request);
}
