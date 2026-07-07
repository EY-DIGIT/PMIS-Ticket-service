package com.pmis.ticket.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient authRestClient;

    /**
     * Calls the user-service introspect endpoint and validates the token is active and unexpired.
     * Returns null when the token is missing, invalid, expired, or the introspect call fails.
     */
    public AuthenticatedUser introspect(String accessToken) {
        try {
            IntrospectResponse resp = authRestClient.post()
                    .body(Map.of("access_token", accessToken))
                    .retrieve()
                    .body(IntrospectResponse.class);

            if (resp == null || resp.getData() == null) return null;

            IntrospectResponse.IntrospectData data = resp.getData();
            if (!data.isActive() || data.isExpired()) return null;
            if (data.getUserId() == null || data.getEmail() == null) return null;

            return new AuthenticatedUser(data.getUserId(), data.getUsername(), data.getEmail(), data.isAdmin());
        } catch (Exception ex) {
            log.warn("Token introspection failed: {}", ex.getMessage());
            return null;
        }
    }
}
