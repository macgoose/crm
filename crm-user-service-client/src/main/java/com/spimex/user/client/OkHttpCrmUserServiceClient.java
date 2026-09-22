package com.spimex.user.client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public final class OkHttpCrmUserServiceClient implements CrmUserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OkHttpCrmUserServiceClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final HttpUrl authorizationUrl;
    private final HttpUrl userResolutionUrl;
    private final Gson gson;

    public OkHttpCrmUserServiceClient(String baseUrl, OkHttpClient httpClient) {
        this(baseUrl, httpClient, new Gson());
    }

    OkHttpCrmUserServiceClient(String baseUrl, OkHttpClient httpClient, Gson gson) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.gson = Objects.requireNonNull(gson, "gson must not be null");
        HttpUrl parsedBaseUrl = HttpUrl.parse(Objects.requireNonNull(baseUrl, "baseUrl must not be null"));
        if (parsedBaseUrl == null) {
            throw new IllegalArgumentException("baseUrl is not a valid HTTP URL");
        }
        this.authorizationUrl = parsedBaseUrl.newBuilder()
                .addPathSegments("internal/v1/authorization-decisions")
                .build();
        this.userResolutionUrl = parsedBaseUrl.newBuilder()
                .addPathSegments("internal/v1/user-resolutions")
                .build();
    }

    @Override
    public AuthorizationResponse authorize(AuthorizationRequest authorizationRequest) {
        Objects.requireNonNull(authorizationRequest, "request must not be null");
        RequestBody body = RequestBody.create(gson.toJson(authorizationRequest), JSON);
        Request request = new Request.Builder()
                .url(authorizationUrl)
                .post(body)
                .header("Accept", "application/json")
                .build();

        log.debug("Calling CRM user service authorization endpoint; permission={}",
                authorizationRequest.getPermission());
        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("CRM user service authorization endpoint responded with status={}", response.code());
            if (!response.isSuccessful()) {
                log.warn("CRM user service authorization endpoint returned status={}", response.code());
                throw new CrmUserServiceHttpException(response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new CrmUserServiceProtocolException("CRM user service returned an empty response");
            }
            AuthorizationResponse result = gson.fromJson(responseBody.charStream(), AuthorizationResponse.class);
            if (result == null) {
                throw new CrmUserServiceProtocolException("CRM user service returned an empty JSON response");
            }
            result.validate();
            log.debug("CRM user service authorization decision received; allowed={} userId={}",
                    result.isAllowed(), result.getUserId());
            return result;
        } catch (JsonParseException exception) {
            log.warn("CRM user service authorization response is invalid JSON", exception);
            throw new CrmUserServiceProtocolException("CRM user service returned invalid JSON", exception);
        } catch (IOException exception) {
            log.warn("CRM user service authorization endpoint is unavailable", exception);
            throw new CrmUserServiceUnavailableException("CRM user service is unavailable", exception);
        }
    }

    @Override
    public UserResolutionResponse resolveUser(UserResolutionRequest userResolutionRequest) {
        Objects.requireNonNull(userResolutionRequest, "request must not be null");
        RequestBody body = RequestBody.create(gson.toJson(userResolutionRequest), JSON);
        Request request = new Request.Builder()
                .url(userResolutionUrl)
                .post(body)
                .header("Accept", "application/json")
                .build();

        log.debug("Calling CRM user service user-resolution endpoint");
        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("CRM user service user-resolution endpoint responded with status={}", response.code());
            if (!response.isSuccessful()) {
                log.warn("CRM user service user-resolution endpoint returned status={}", response.code());
                throw new CrmUserServiceHttpException(response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new CrmUserServiceProtocolException("CRM user service returned an empty response");
            }
            UserResolutionResponse result = gson.fromJson(
                    responseBody.charStream(), UserResolutionResponse.class);
            if (result == null) {
                throw new CrmUserServiceProtocolException("CRM user service returned an empty JSON response");
            }
            result.validate();
            log.debug("CRM user service user-resolution result received; resolved={} userId={}",
                    result.isResolved(), result.getUserId());
            return result;
        } catch (JsonParseException exception) {
            log.warn("CRM user service user-resolution response is invalid JSON", exception);
            throw new CrmUserServiceProtocolException("CRM user service returned invalid JSON", exception);
        } catch (IOException exception) {
            log.warn("CRM user service user-resolution endpoint is unavailable", exception);
            throw new CrmUserServiceUnavailableException("CRM user service is unavailable", exception);
        }
    }
}
