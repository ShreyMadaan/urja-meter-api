package com.flock.urja.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "urja.portal")
public record PortalProperties(String baseUrl, String email, String password) {}
