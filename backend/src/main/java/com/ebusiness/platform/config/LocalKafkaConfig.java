package com.ebusiness.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("local")
public class LocalKafkaConfig {

    // This configuration disables Kafka for local testing
    // Kafka is marked as disabled in application-local.yml
    // The application will still work without Kafka for basic functionality
    
    // For testing message publishing, we can use in-memory alternatives
    // or simple logging to verify the flow works
}