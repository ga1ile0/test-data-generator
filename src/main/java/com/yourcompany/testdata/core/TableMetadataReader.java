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
 * Reads column metadata from the database for a given schema and table.
 * Stateless: a new connection is obtained per invocation, making this safe as a singleton.
 */
public class TableMetadataReader {

    private final DataSource dataSource;

    public TableMetadataReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Reads column metadata for {@code schema.tableName}.
     *
     * @param schema    Oracle schema (owner); may be {@code null} to use the session default
     * @param tableName table name (will be upper-cased before querying)
     * @return columns sorted by ordinal position
     * @throws SQLException             if JDBC access fails
     * @throws IllegalArgumentException if the table has no columns (table not found)
     */
    public List<ColumnMetadata> read(String schema, String tableName) throws SQLException {
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
