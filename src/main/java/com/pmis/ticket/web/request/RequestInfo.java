package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestInfo {
    private UserInfo userInfo;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private String uuid;
        private String userName;
        private String email;
        private List<RoleInfo> roles;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RoleInfo {
        private String code;
    }
}
