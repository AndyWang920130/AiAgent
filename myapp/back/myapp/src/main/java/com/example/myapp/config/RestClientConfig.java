package com.example.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Dedicated {@link RestClient} for the Data Integration "execute" feature, with
 * bounded connect/read timeouts so a slow or hung upstream can't stall the request
 * thread. Kept separate from any other HTTP usage so these timeouts don't leak.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient dataIntegrationRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}
