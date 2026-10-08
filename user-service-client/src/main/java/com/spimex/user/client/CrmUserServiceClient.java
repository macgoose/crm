package com.spimex.user.client;

public interface CrmUserServiceClient {

    UserResponse getUser(java.util.UUID userId);

    AuthorizationResponse authorize(AuthorizationRequest request);

    UserResolutionResponse resolveUser(UserResolutionRequest request);
}
