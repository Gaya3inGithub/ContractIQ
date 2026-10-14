package com.contractiq.test;

import com.contractiq.config.ProductionAccessFilter;
import com.contractiq.util.EnvLoader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class ProductionAccessFilterTest {
    private Map<String, String> originalEnvironment;

    @AfterEach
    void restoreEnvironment() throws Exception {
        if (originalEnvironment != null) {
            environment().clear();
            environment().putAll(originalEnvironment);
        }
    }

    @Test
    void developmentRequestsRetainTheExistingUnauthenticatedWorkflow() throws Exception {
        configure("development", "", "");
        FilterChain chain = mock(FilterChain.class);

        new ProductionAccessFilter().doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void productionWithoutCredentialsFailsClosed() throws Exception {
        configure("production", "", "");
        HttpServletResponse response = responseWithWriter();
        FilterChain chain = mock(FilterChain.class);

        new ProductionAccessFilter().doFilter(mock(HttpServletRequest.class), response, chain);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verifyNoInteractions(chain);
    }

    @Test
    void validProductionBasicAuthAllowsTheRequest() throws Exception {
        configure("production", "reviewer", "safe-test-password");
        HttpServletRequest request = mock(HttpServletRequest.class);
        String encoded = Base64.getEncoder().encodeToString("reviewer:safe-test-password".getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encoded);
        FilterChain chain = mock(FilterChain.class);

        new ProductionAccessFilter().doFilter(request, responseWithWriter(), chain);

        verify(chain).doFilter(eq(request), any());
    }

    private HttpServletResponse responseWithWriter() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }

    private void configure(String environmentName, String username, String password) throws Exception {
        Map<String, String> environment = environment();
        originalEnvironment = new HashMap<>(environment);
        environment.put("APP_ENV", environmentName);
        environment.put("APP_BASIC_AUTH_USERNAME", username);
        environment.put("APP_BASIC_AUTH_PASSWORD", password);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> environment() throws Exception {
        Field field = EnvLoader.class.getDeclaredField("envMap");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }
}
