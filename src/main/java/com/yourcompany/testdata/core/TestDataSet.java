package com.yourcompany.testdata.core;

import com.yourcompany.testdata.override.ColumnOverride;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Configuration for a single test-data generation run.
 * Build with {@link TestDataSet#builder()}, then pass to {@link com.yourcompany.testdata.TestDataGenerator}.
 * <p>
 * This class is a pure value object — no DB access happens at build time.
 */
@Builder
@Getter
public class TestDataSet {

    private final String schema;
    private final String tableName;
    @Builder.Default
    private final int rowCount = 1;
    @Singular
    private final List<ColumnOverride> overrides;
}
