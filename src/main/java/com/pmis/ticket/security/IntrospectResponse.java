package com.pmis.ticket.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Maps the response body of POST /users/api/v3/users/introspect. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectResponse {

    private IntrospectData data;
    private String message;
    private String error;
    private int status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntrospectData {
        private boolean active;
        private boolean expired;
        private String sub;
        private String username;
        private String userId;
        private String email;
        private boolean isAdmin;
    }
}
