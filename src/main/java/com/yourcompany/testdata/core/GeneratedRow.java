package com.yourcompany.testdata.core;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable snapshot of one generated table row.
 * Column names are always stored upper-cased.
 */
@Getter
public class GeneratedRow {

    private final Map<String, Object> values;

    public GeneratedRow(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** Returns the value for the given column name (case-insensitive). */
    public Object get(String columnName) {
        return values.get(columnName.toUpperCase());
    }
}
