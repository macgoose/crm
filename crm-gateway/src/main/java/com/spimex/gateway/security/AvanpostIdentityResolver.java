package com.spimex.gateway.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@Component
@Slf4j
public class AvanpostIdentityResolver {

    private final IdentityProperties properties;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    private final Type userInfoType = new TypeToken<Map<String, Object>>() {}.getType();

    public AvanpostIdentityResolver(
        IdentityProperties properties,
        OkHttpClient httpClient
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public ExternalIdentity resolve(Jwt jwt, String bearerToken) {
        log.debug("Resolving external identity using source={}", properties.getSource());
        if (properties.getSource() == IdentityProperties.Source.USERINFO) {
            log.debug("Identity source requires userinfo request");
            return resolveFromUserInfo(bearerToken);
        }

        ExternalIdentity identity = extract(jwt.getClaims());

        if (hasIdentity(identity)) {
            log.debug("Usable identity found in JWT: loginPresent={} emailPresent={}",
                identity.login() != null, identity.email() != null);
            return identity;
        }

        if (properties.getSource() == IdentityProperties.Source.JWT)
            return requireIdentity(identity);

        log.debug("JWT has no usable identity; falling back to userinfo");
        return resolveFromUserInfo(bearerToken);
    }

    private ExternalIdentity resolveFromUserInfo(String bearerToken) {
        return requireIdentity(extract(loadUserInfo(bearerToken)));
    }

    private Map<String, ?> loadUserInfo(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank())
            throw new IdentityResolutionException(
                "Bearer token is required for userinfo"
            );

        Request request = createUserInfoRequest(bearerToken);

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Userinfo endpoint responded with status={}", response.code());
            if (!response.isSuccessful())
                throw new IdentityProviderUnavailableException(
                    "Avanpost userinfo returned HTTP " + response.code()
                );

            ResponseBody body = response.body();

            if (body == null)
                throw new IdentityProviderUnavailableException(
                    "Avanpost userinfo returned an empty response"
                );

            JsonObject json = gson.fromJson(body.charStream(), JsonObject.class);

            if (json == null)
                throw new IdentityProviderUnavailableException(
                    "Avanpost userinfo returned an empty JSON response"
                );

            return gson.fromJson(json, userInfoType);

        } catch (IOException | JsonParseException exception) {
            throw new IdentityProviderUnavailableException(
                "Avanpost userinfo is unavailable",
                exception
            );
        }
    }

    private Request createUserInfoRequest(String bearerToken) {
        return new Request.Builder()
            .url(properties.getUserInfoUri().toString())
            .header("Authorization", "Bearer " + bearerToken)
            .header("Accept", "application/json")
            .get()
            .build();
    }

    private ExternalIdentity extract(Map<String, ?> claims) {
        String login = stringValue(
            claims.get(properties.getLoginClaim())
        );

        String email = stringValue(
            claims.get(properties.getEmailClaim())
        );

        boolean emailVerified = booleanValue(
            claims.get(properties.getEmailVerifiedClaim())
        );

        if (!properties.isTrustUnverifiedEmail() && !emailVerified)
            email = null;

        return new ExternalIdentity(login, email);
    }

    private ExternalIdentity requireIdentity(ExternalIdentity identity) {
        if (!hasIdentity(identity))
            throw new IdentityResolutionException(
                "Token does not contain a usable login or verified email"
            );

        return identity;
    }

    private static boolean hasIdentity(ExternalIdentity identity) {
        return identity != null
            && (identity.login() != null || identity.email() != null);
    }

    private static String stringValue(Object value) {
        if (!(value instanceof String string) || string.isBlank())
            return null;

        return string.trim();
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }
}
