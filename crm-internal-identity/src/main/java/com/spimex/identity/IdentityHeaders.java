package com.spimex.identity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.spimex.identity.IdentityHeaderException.Reason.*;

public final class IdentityHeaders {

    public static final String USER_ID = "X-Internal-Identity-User-Id";
    public static final String USER_VERSION = "X-Internal-Identity-Version";

    private IdentityHeaders() {
    }

    public static InternalIdentity read(HeaderSource headers) {
        String userIdValue = required(headers.firstValue(USER_ID), MISSING_USER_ID, USER_ID);
        String versionValue = required(headers.firstValue(USER_VERSION), MISSING_USER_VERSION, USER_VERSION);

        UUID userId;
        try {
            userId = UUID.fromString(userIdValue);
        } catch (IllegalArgumentException exception) {
            throw new IdentityHeaderException(INVALID_USER_ID, USER_ID + " must contain a UUID");
        }

        long userVersion;
        try {
            userVersion = Long.parseLong(versionValue);
        } catch (NumberFormatException exception) {
            throw new IdentityHeaderException(INVALID_USER_VERSION, USER_VERSION + " must contain a number");
        }
        if (userVersion < 0) {
            throw new IdentityHeaderException(INVALID_USER_VERSION, USER_VERSION + " must not be negative");
        }

        return new InternalIdentity(userId, userVersion);
    }

    public static Map<String, String> write(InternalIdentity identity) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(USER_ID, identity.getUserId().toString());
        result.put(USER_VERSION, Long.toString(identity.getUserVersion()));
        return Collections.unmodifiableMap(result);
    }

    private static String required(String value, IdentityHeaderException.Reason reason, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IdentityHeaderException(reason, name + " is required");
        }
        return value.trim();
    }
}
