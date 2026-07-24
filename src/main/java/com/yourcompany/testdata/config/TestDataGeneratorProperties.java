package com.yourcompany.testdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the test-data-generator library.
 * Prefix: {@code testdata.generator}.
 */
@ConfigurationProperties(prefix = "testdata.generator")
public class TestDataGeneratorProperties {

    /** Optional default Oracle schema to use when none is explicitly specified. */
    private String defaultSchema;

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }
}
