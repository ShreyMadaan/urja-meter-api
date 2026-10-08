package com.flock.urja;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
@SpringBootApplication
@ConfigurationPropertiesScan
public class UrjaMeterApiApplication {
    public static void main(String[] args) { SpringApplication.run(UrjaMeterApiApplication.class, args); }
}
