package com.pmis.ticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** Full URL of the user-service introspect endpoint. */
    private String introspectUrl;

    /** TCP connect timeout in milliseconds. */
    private int connectTimeoutMs = 3000;

    /** Socket read timeout in milliseconds. */
    private int readTimeoutMs = 5000;
}
