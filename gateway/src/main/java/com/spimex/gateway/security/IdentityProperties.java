package com.spimex.gateway.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "crm.identity")
public class IdentityProperties {

    public enum Source {
        JWT,
        USERINFO,
        JWT_WITH_USERINFO_FALLBACK
    }

    private Source source = Source.JWT;
    private String loginClaim = "preferred_username";
    private String emailClaim = "email";
    private String emailVerifiedClaim = "email_verified";
    private boolean trustUnverifiedEmail;
    private URI userInfoUri = URI.create("http://localhost:9090/oauth2/userinfo");

}
