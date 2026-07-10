package com.pmis.ticket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableAsync
@EnableConfigurationProperties({NotificationProperties.class, AuthProperties.class})
public class AppConfig {

    /**
     * Runs as a real servlet Filter (HIGHEST_PRECEDENCE) so it executes before
     * AuthenticationFilter — preflight OPTIONS requests get answered here directly,
     * and CORS headers are attached to every response, including 401s written by
     * AuthenticationFilter further down the chain.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
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

    /** RestClient wired to the user-service introspect endpoint. */
    @Bean
    public RestClient authRestClient(AuthProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getIntrospectUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
