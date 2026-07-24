package com.yourcompany.testdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the test-data-generator library.
 * Prefix: {@code testdata.generator}.
 */
@ConfigurationProperties(prefix = "testdata.generator")
@Getter
@Setter
public class TestDataGeneratorProperties {

    /** Optional default Oracle schema to use when none is explicitly specified. */
    private String defaultSchema;
}
