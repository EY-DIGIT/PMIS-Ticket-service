package com.pmis.ticket.security;

/** Identity established from a validated introspect call. */
public record AuthenticatedUser(String userId, String username, String email, boolean isAdmin) {
}
