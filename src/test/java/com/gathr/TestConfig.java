package com.gathr;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.info.BuildProperties;
import java.util.Properties;
import java.time.Instant;

@TestConfiguration
public class TestConfig {

    @Bean
    public BuildProperties buildProperties() {
        Properties entries = new Properties();
        entries.put("version", "0.0.1-SNAPSHOT");
        entries.put("name", "gathr-backend");
        entries.put("time", Instant.now().toString());
        return new BuildProperties(entries);
    }
}
