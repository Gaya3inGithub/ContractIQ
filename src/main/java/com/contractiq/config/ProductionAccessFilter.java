package com.contractiq.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Prevents a production deployment from exposing contract data or reviewer
 * actions unless a deployment-specific Basic Auth credential is configured.
 * Development retains the existing assignment workflow without authentication.
 */
@WebFilter(urlPatterns = "/*")
public class ProductionAccessFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!AppConfig.isProduction()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Cache-Control", "no-store");

        if (!AppConfig.isBasicAuthConfigured()) {
            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResponse.getWriter().write("Production access protection is not configured.");
            return;
        }

        String authorization = ((HttpServletRequest) request).getHeader("Authorization");
        if (isAuthorized(authorization)) {
            chain.doFilter(request, response);
            return;
        }

        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"ContractIQ\", charset=\"UTF-8\"");
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.getWriter().write("Authentication is required.");
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }

            byte[] suppliedUsername = decoded.substring(0, separator).getBytes(StandardCharsets.UTF_8);
            byte[] suppliedPassword = decoded.substring(separator + 1).getBytes(StandardCharsets.UTF_8);
            byte[] expectedUsername = AppConfig.getBasicAuthUsername().getBytes(StandardCharsets.UTF_8);
            byte[] expectedPassword = AppConfig.getBasicAuthPassword().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedUsername, suppliedUsername)
                    && MessageDigest.isEqual(expectedPassword, suppliedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
