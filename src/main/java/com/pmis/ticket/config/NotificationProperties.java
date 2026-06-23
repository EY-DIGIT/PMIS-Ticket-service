package com.pmis.ticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    /** Master on/off switch — set to false to suppress all outbound notifications. */
    private boolean enabled = true;

    /** Full URL of the email notification endpoint. */
    private String url;

    /** Bearer / API-key token sent as Authorization header. Empty = no header sent. */
    private String authToken = "";

    /** TCP connect timeout in milliseconds. */
    private int connectTimeoutMs = 3000;

    /** Socket read timeout in milliseconds. */
    private int readTimeoutMs = 5000;
}
