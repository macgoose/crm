package com.spimex.gateway.api;

import com.spimex.identity.IdentityHeaders;
import com.spimex.identity.InternalIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

final class TrustedIdentityRequest extends HttpServletRequestWrapper {

    private final Map<String, String> trustedHeaders;

    TrustedIdentityRequest(HttpServletRequest request, InternalIdentity identity) {
        super(request);
        this.trustedHeaders = IdentityHeaders.write(identity);
    }

    @Override
    public String getHeader(String name) {
        String trusted = trustedValue(name);
        return trusted != null ? trusted : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String trusted = trustedValue(name);
        return trusted != null
                ? Collections.enumeration(List.of(trusted))
                : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<String> originalNames = super.getHeaderNames();
        while (originalNames != null && originalNames.hasMoreElements()) {
            String name = originalNames.nextElement();
            if (!isInternalIdentityHeader(name)) {
                names.add(name);
            }
        }
        names.addAll(trustedHeaders.keySet());
        return Collections.enumeration(new ArrayList<>(names));
    }

    private String trustedValue(String name) {
        for (Map.Entry<String, String> entry : trustedHeaders.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isInternalIdentityHeader(String name) {
        return IdentityHeaders.USER_ID.equalsIgnoreCase(name)
                || IdentityHeaders.USER_VERSION.equalsIgnoreCase(name);
    }
}
