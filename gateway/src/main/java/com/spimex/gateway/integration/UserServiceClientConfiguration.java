package com.spimex.gateway.integration;

import com.spimex.user.client.CrmUserServiceClient;
import com.spimex.user.client.OkHttpCrmUserServiceClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "crm.user-service")
public class UserServiceClientConfiguration {

    private String baseUrl = "http://localhost:8081";

    @Bean
    CrmUserServiceClient crmUserServiceClient() {
        return new OkHttpCrmUserServiceClient(getBaseUrl());
    }
}
