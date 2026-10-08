package com.flock.urja.service;

import tools.jackson.databind.JsonNode;
import com.flock.urja.client.PortalClient;
import com.flock.urja.error.PortalException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MeterService {
    private final PortalClient portal;
    public MeterService(PortalClient portal) { this.portal = portal; }

    public JsonNode search(String query, int page) {
        return portal.getJson("/portal/meters/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=" + page);
    }

    // Export provides structured metadata, installation type, network hierarchy and coordinates.
    // It is fetched on demand to avoid silently serving stale data; the tradeoff is higher latency.
    public JsonNode details(String meterId) {
        JsonNode data = portal.getJson("/portal/export?page=1").path("data");
        if (!data.isArray()) throw new PortalException(HttpStatus.BAD_GATEWAY, "Unexpected export format");
        for (JsonNode meter : data) {
            if (meterId.equals(meter.path("meterId").asText())) return meter;
        }
        throw new PortalException(HttpStatus.NOT_FOUND, "Meter not found in export");
    }

    public JsonNode network(String meterId) {
        JsonNode hierarchy = details(meterId).path("hierarchy");
        if (hierarchy.isMissingNode() || hierarchy.isNull()) throw new PortalException(HttpStatus.BAD_GATEWAY, "Meter hierarchy unavailable");
        return hierarchy;
    }

    public JsonNode location(String meterId) {
        JsonNode response = portal.getJson("/portal/meters/" + meterId + "/geo");
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) throw new PortalException(HttpStatus.BAD_GATEWAY, "Meter location unavailable");
        return data;
    }

    public JsonNode consumption(String meterId) {
        JsonNode response = portal.getJson("/portal/meters/" + meterId + "/energy");
        JsonNode data = response.path("data");
        if (!data.isArray()) throw new PortalException(HttpStatus.BAD_GATEWAY, "Meter readings unavailable");
        return data;
    }
}
