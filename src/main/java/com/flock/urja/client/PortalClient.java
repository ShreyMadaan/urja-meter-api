package com.flock.urja.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.flock.urja.auth.PortalSessionManager;
import com.flock.urja.config.PortalProperties;
import com.flock.urja.error.PortalException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PortalClient {
    private final RestClient restClient;
    private final PortalSessionManager sessions;
    private final ObjectMapper mapper;

    public PortalClient(PortalProperties properties, PortalSessionManager sessions, ObjectMapper mapper) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.sessions = sessions;
        this.mapper = mapper;
    }

    public JsonNode getJson(String path) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String cookie = sessions.getSessionCookie();
            try {
                String response = restClient.get().uri(path)
                        .header(HttpHeaders.COOKIE, cookie).retrieve().body(String.class);
                if (response == null || response.isBlank()) throw new PortalException(HttpStatus.BAD_GATEWAY, "Empty portal response");
                return mapper.readTree(response);
            } catch (HttpClientErrorException.Unauthorized ex) {
                sessions.invalidateIfCurrent(cookie);
                if (attempt == 1) throw new PortalException(HttpStatus.BAD_GATEWAY, "Portal rejected renewed session");
            } catch (HttpClientErrorException.NotFound ex) {
                throw new PortalException(HttpStatus.NOT_FOUND, "Upstream resource not found");
            } catch (RestClientException ex) {
                throw new PortalException(HttpStatus.BAD_GATEWAY, "Portal request failed");
            } catch (tools.jackson.core.JacksonException ex) {
                throw new PortalException(HttpStatus.BAD_GATEWAY, "Invalid portal JSON response");
            }
        }
        throw new PortalException(HttpStatus.BAD_GATEWAY, "Portal request failed");
    }
}
