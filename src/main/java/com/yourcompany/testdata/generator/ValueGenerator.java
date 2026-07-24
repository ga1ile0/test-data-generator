package com.yourcompany.testdata.generator;

import com.yourcompany.testdata.core.ColumnMetadata;

/**
 * Strategy interface for generating a single column value.
 */
public interface ValueGenerator {
    Object generate(ColumnMetadata column);
}
