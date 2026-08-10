package com.academy.mudogroupware.global.infrastructure.web;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private static final String IPV4_MAPPED_IPV6_PREFIX = "::ffff:";

    public String resolve(HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        if (ipAddress != null && ipAddress.startsWith(IPV4_MAPPED_IPV6_PREFIX)) {
            return ipAddress.substring(IPV4_MAPPED_IPV6_PREFIX.length());
        }
        return ipAddress;
    }
}
