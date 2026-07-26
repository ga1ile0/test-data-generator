package com.yourcompany.testdata;

import com.yourcompany.testdata.core.ColumnMetadata;
import com.yourcompany.testdata.core.GeneratedRow;
import com.yourcompany.testdata.core.TableMetadataReader;
import com.yourcompany.testdata.core.TestDataSet;
import com.yourcompany.testdata.generator.OracleValueGenerator;
import com.yourcompany.testdata.generator.ValueGenerator;
import com.yourcompany.testdata.output.DbInserter;
import com.yourcompany.testdata.output.SqlScriptExporter;
import com.yourcompany.testdata.override.ColumnOverride;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary entry point for generating and persisting Oracle test data.
 *
 * <p>Instantiate with a {@link DataSource}; the default value generator is
 * {@link OracleValueGenerator}. Supply a custom {@link ValueGenerator} via the
 * two-argument constructor to override generation behaviour.
 *
 * <pre>{@code
 * TestDataGenerator gen = new TestDataGenerator(dataSource);
 *
 * List<GeneratedRow> rows = gen.insertToDb(
 *     gen.forTable("EMPLOYEES")
 *        .schema("HR")
 *        .rows(10)
 *        .override(ColumnOverride.fixed("STATUS", "ACTIVE"))
 *        .build());
 * }</pre>
 *
 * <p>Thread safety: the instance is safe to share across threads provided the
 * underlying {@link DataSource} is thread-safe (all standard connection pools are).
 */
public class TestDataGenerator {

    private final DataSource dataSource;
    private final ValueGenerator valueGenerator;

    /**
     * Creates a generator using the default {@link OracleValueGenerator}.
     *
     * @param dataSource JDBC data source for metadata reads and inserts
     */
    public TestDataGenerator(DataSource dataSource) {
        this(dataSource, new OracleValueGenerator());
    }

    /**
     * Creates a generator with a custom value generator.
     *
     * @param dataSource     JDBC data source for metadata reads and inserts
     * @param valueGenerator strategy for producing column values
     */
    public TestDataGenerator(DataSource dataSource, ValueGenerator valueGenerator) {
        this.dataSource = dataSource;
        this.valueGenerator = valueGenerator;
    }

    /**
     * Returns a fluent builder pre-set with {@code tableName}.
     * No DB access happens until you call {@code generate()}, {@code insertToDb()}, etc.
     *
     * @param tableName target Oracle table name
     * @return a builder ready for further configuration
     */
    public TestDataSet.TestDataSetBuilder forTable(String tableName) {
        return TestDataSet.builder().tableName(tableName);
    }

    /**
     * Reads column metadata and generates rows according to {@code dataset}.
     *
     * @param dataset configured test data set
     * @return the generated rows
     * @throws SQLException if metadata reading fails
     */
    public List<GeneratedRow> generate(TestDataSet dataset) throws SQLException {
        List<ColumnMetadata> columns = TableMetadataReader.read(
                dataSource, dataset.getSchema(), dataset.getTableName());
        return doGenerate(dataset, columns);
    }

    /**
     * Generates rows and inserts them into the database.
     *
     * @param dataset configured test data set
     * @return the generated rows (same list that was inserted)
     * @throws SQLException if metadata reading or any INSERT fails
     */
    public List<GeneratedRow> insertToDb(TestDataSet dataset) throws SQLException {
        List<ColumnMetadata> columns = TableMetadataReader.read(
                dataSource, dataset.getSchema(), dataset.getTableName());
        List<GeneratedRow> rows = doGenerate(dataset, columns);
        DbInserter.insert(dataSource, dataset.getSchema(), dataset.getTableName(), rows, columns);
        return rows;
    }

    /**
     * Generates rows and writes them as Oracle SQL INSERT statements to {@code outputPath}.
     *
     * @param dataset    configured test data set
     * @param outputPath destination file for the SQL script
     * @return the generated rows
     * @throws SQLException if metadata reading fails
     * @throws IOException  if the file cannot be written
     */
    public List<GeneratedRow> exportToSql(TestDataSet dataset, Path outputPath)
            throws SQLException, IOException {
        List<ColumnMetadata> columns = TableMetadataReader.read(
                dataSource, dataset.getSchema(), dataset.getTableName());
        List<GeneratedRow> rows = doGenerate(dataset, columns);
        SqlScriptExporter.export(dataset.getSchema(), dataset.getTableName(), rows, columns, outputPath);
        return rows;
    }

    /**
     * Generates rows, inserts them into the database, and writes them to a SQL script.
     * Both outputs receive the exact same generated rows.
     *
     * @param dataset    configured test data set
     * @param outputPath destination file for the SQL script
     * @return the generated rows
     * @throws SQLException if metadata reading or any INSERT fails
     * @throws IOException  if the file cannot be written
     */
    public List<GeneratedRow> insertAndExport(TestDataSet dataset, Path outputPath)
            throws SQLException, IOException {
        List<ColumnMetadata> columns = TableMetadataReader.read(
                dataSource, dataset.getSchema(), dataset.getTableName());
        List<GeneratedRow> rows = doGenerate(dataset, columns);
        DbInserter.insert(dataSource, dataset.getSchema(), dataset.getTableName(), rows, columns);
        SqlScriptExporter.export(dataset.getSchema(), dataset.getTableName(), rows, columns, outputPath);
        return rows;
    }

    // -------------------------------------------------------------------------

    private List<GeneratedRow> doGenerate(TestDataSet dataset, List<ColumnMetadata> columns) {
        int count = dataset.getRowCount() > 0 ? dataset.getRowCount() : 1;
        List<GeneratedRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (ColumnMetadata col : columns) {
                String upperName = col.columnName().toUpperCase();
                ColumnOverride override = findOverride(dataset.getOverrides(), upperName);
                values.put(upperName, override != null
                        ? override.nextValue()
                        : valueGenerator.generate(col));
            }
            rows.add(new GeneratedRow(values));
        }
        return rows;
    }

    private static ColumnOverride findOverride(List<ColumnOverride> overrides, String upperName) {
        for (ColumnOverride o : overrides) {
            if (o.getColumnName().equals(upperName)) return o;
        }
        return null;
    }
}
