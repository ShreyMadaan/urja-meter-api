package com.flock.urja.controller;
import tools.jackson.databind.JsonNode;
import com.flock.urja.client.PortalClient;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@Validated
@RequestMapping("/api/v1/transformers")
public class TransformerController {
    private final PortalClient portal;
    public TransformerController(PortalClient portal) { this.portal = portal; }
    @GetMapping
    public JsonNode list(@RequestParam(defaultValue = "1") @Min(1) int page) {
        return portal.getJson("/portal/dts?page=" + page);
    }
}
