package com.yourcompany.testdata.core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Static utility for reading Oracle column metadata from JDBC.
 * A new connection is obtained per call — stateless and thread-safe.
 */
public final class TableMetadataReader {

    private TableMetadataReader() {}

    /**
     * Reads column metadata for {@code schema.tableName}.
     *
     * @param dataSource JDBC data source
     * @param schema     Oracle schema (owner); may be {@code null} to use the session default
     * @param tableName  table name (upper-cased before querying)
     * @return columns sorted by ordinal position
     * @throws SQLException             if JDBC access fails
     * @throws IllegalArgumentException if the table has no columns (table not found)
     */
    public static List<ColumnMetadata> read(DataSource dataSource, String schema, String tableName)
            throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, schema, tableName.toUpperCase(), "%")) {
                while (rs.next()) {
                    columns.add(new ColumnMetadata(
                            rs.getString("COLUMN_NAME"),
                            rs.getInt("DATA_TYPE"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("DECIMAL_DIGITS"),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                            rs.getInt("ORDINAL_POSITION")
                    ));
                }
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "No columns found for table: " + (schema != null ? schema + "." : "") + tableName);
        }
        columns.sort(Comparator.comparingInt(ColumnMetadata::ordinalPosition));
        return columns;
    }
}
