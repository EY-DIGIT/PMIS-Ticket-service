package com.pmis.ticket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@EnableAsync
@EnableConfigurationProperties(NotificationProperties.class)
public class AppConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }

    /**
     * RestClient wired to the external notification API.
     * Timeouts are read from application.properties / env vars.
     * Auth token is sent as Authorization header when non-blank.
     */
    @Bean
    public RestClient notificationRestClient(NotificationProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getUrl());

        if (props.getAuthToken() != null && !props.getAuthToken().isBlank()) {
            builder.defaultHeader("Authorization", props.getAuthToken());
        }

        return builder.build();
    }
}
