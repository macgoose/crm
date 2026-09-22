package com.spimex.user.client;

public interface CrmUserServiceClient {

    AuthorizationResponse authorize(AuthorizationRequest request);

    UserResolutionResponse resolveUser(UserResolutionRequest request);
}
