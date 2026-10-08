package com.flock.urja.auth;

import com.flock.urja.config.PortalProperties;
import com.flock.urja.error.PortalException;
import java.net.HttpCookie;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PortalSessionManager {
    private static final String COOKIE_NAME = "__Secure-better-auth.session_token";
    private final PortalProperties properties;
    private final RestClient restClient;
    private String cookieHeader;
    private Instant expiresAt;

    public PortalSessionManager(PortalProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public synchronized String getSessionCookie() {
        if (cookieHeader == null || expiresAt == null || !Instant.now().isBefore(expiresAt))
            authenticate();
        return cookieHeader;
    }

    // Only invalidate the cookie that actually failed; avoid erasing a session
    // refreshed by another thread.
    public synchronized void invalidateIfCurrent(String failedCookie) {
        if (failedCookie.equals(cookieHeader)) {
            cookieHeader = null;
            expiresAt = null;
        }
    }

    private void authenticate() {
        if (properties.email() == null || properties.email().isBlank()
                || properties.password() == null || properties.password().isBlank()) {
            throw new PortalException(HttpStatus.SERVICE_UNAVAILABLE, "Portal credentials are not configured");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", properties.email());
        form.add("password", properties.password());
        try {
            var response = restClient.post().uri("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
            HttpCookie session = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                    .flatMap(h -> HttpCookie.parse(h).stream())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .findFirst().orElseThrow(() -> new PortalException(HttpStatus.BAD_GATEWAY,
                            "Portal login did not return a session cookie"));
            if (session.getMaxAge() <= 0 || session.getValue().isBlank()) {
                throw new PortalException(HttpStatus.BAD_GATEWAY, "Portal returned an invalid session cookie");
            }
            cookieHeader = session.getName() + "=" + session.getValue();
            expiresAt = Instant.now().plusSeconds(Math.max(1, session.getMaxAge() - 30));
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Portal login HTTP request failed", ex);
        }
    }
}
