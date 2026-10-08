package com.flock.urja.controller;

import tools.jackson.databind.JsonNode;
import com.flock.urja.service.MeterService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/meters")
public class MeterController {
    private final MeterService meters;
    public MeterController(MeterService meters) { this.meters = meters; }

    @GetMapping
    public JsonNode search(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "1") @Min(1) int page) {
        return meters.search(q, page);
    }

    @GetMapping("/{meterId}")
    public JsonNode details(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String meterId) {
        return meters.details(meterId);
    }

    @GetMapping("/{meterId}/network")
    public JsonNode network(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String meterId) {
        return meters.network(meterId);
    }

    @GetMapping("/{meterId}/location")
    public JsonNode location(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String meterId) {
        return meters.location(meterId);
    }

    @GetMapping("/{meterId}/consumption")
    public JsonNode consumption(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String meterId) {
        return meters.consumption(meterId);
    }
}
