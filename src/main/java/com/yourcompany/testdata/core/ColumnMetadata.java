package com.yourcompany.testdata.core;

/**
 * Immutable metadata for a single database column, sourced from
 * {@link java.sql.DatabaseMetaData#getColumns}.
 *
 * @param columnName      column name (upper-cased)
 * @param sqlType         {@link java.sql.Types} constant
 * @param typeName        Oracle-native type name (e.g. VARCHAR2, NUMBER, DATE)
 * @param columnSize      max char length or total digits for NUMBER
 * @param decimalDigits   scale for NUMBER columns
 * @param nullable        whether the column accepts NULL
 * @param ordinalPosition 1-based position in the table definition
 */
public record ColumnMetadata(
        String columnName,
        int sqlType,
        String typeName,
        int columnSize,
        int decimalDigits,
        boolean nullable,
        int ordinalPosition
) {}
