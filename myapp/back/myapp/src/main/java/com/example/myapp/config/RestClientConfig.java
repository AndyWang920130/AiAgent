package com.example.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Dedicated {@link RestClient} beans for features that call out to third parties, each
 * with bounded connect/read timeouts so a slow or hung upstream can't stall the request
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

    /**
     * Client for the public stock quote APIs. Timeouts are tighter than Data Integration's
     * because a quote refresh happens while a user waits on the Stock List page, and a
     * stale price is a better outcome than a hanging page.
     */
    @Bean
    public RestClient stockQuoteRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(6));
        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}
