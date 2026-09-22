package com.spimex.gateway.user;

import com.spimex.user.client.CrmUserServiceClient;
import com.spimex.user.client.OkHttpCrmUserServiceClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceClientConfiguration {

    @Bean
    OkHttpClient internalHttpClient(UserServiceProperties properties) {
        return new OkHttpClient.Builder()
            .connectTimeout(properties.getConnectTimeout())
            .readTimeout(properties.getReadTimeout())
            .retryOnConnectionFailure(false)
            .build();
    }

    @Bean
    CrmUserServiceClient crmUserServiceClient(
        UserServiceProperties properties,
        OkHttpClient internalHttpClient
    ) {
        return new OkHttpCrmUserServiceClient(properties.getBaseUrl(), internalHttpClient);
    }
}
