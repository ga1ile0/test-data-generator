package com.yourcompany.testdata;

import com.yourcompany.testdata.core.GeneratedRow;
import com.yourcompany.testdata.core.TableMetadataReader;
import com.yourcompany.testdata.core.TestDataSet;
import com.yourcompany.testdata.generator.ValueGenerator;
import com.yourcompany.testdata.output.DbInserter;
import com.yourcompany.testdata.output.SqlScriptExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Primary entry point for generating and persisting test data.
 *
 * <p>Inject this bean in your tests:
 * <pre>{@code
 * @Autowired
 * TestDataGenerator generator;
 *
 * void myTest() throws Exception {
 *     List<GeneratedRow> rows = generator.insertToDb(
 *         generator.forTable("EMPLOYEES").schema("HR").rows(10).build());
 * }
 * }</pre>
 *
 * <p>Thread safety: all collaborators are stateless; this class is safe as a singleton.
 */
public class TestDataGenerator {

    private final TableMetadataReader metadataReader;
    private final ValueGenerator valueGenerator;
    private final DbInserter dbInserter;
    private final SqlScriptExporter sqlScriptExporter;

    public TestDataGenerator(TableMetadataReader metadataReader,
                             ValueGenerator valueGenerator,
                             DbInserter dbInserter,
                             SqlScriptExporter sqlScriptExporter) {
        this.metadataReader = metadataReader;
        this.valueGenerator = valueGenerator;
        this.dbInserter = dbInserter;
        this.sqlScriptExporter = sqlScriptExporter;
    }

    /**
     * Creates a pre-wired {@link TestDataSet.Builder} for {@code tableName}.
     * Call {@link TestDataSet.Builder#build()} after configuring the builder to
     * eagerly resolve column metadata.
     *
     * @param tableName target Oracle table name
     * @return a fluent builder ready for further configuration
     */
    public TestDataSet.Builder forTable(String tableName) {
        return new TestDataSet.Builder()
                .table(tableName)
                .reader(metadataReader)
                .generator(valueGenerator);
    }

    /**
     * Generates rows from {@code dataset} and inserts them into the database.
     *
     * @param dataset configured test data set
     * @return the generated rows (same list that was inserted)
     * @throws SQLException if the INSERT fails
     */
    public List<GeneratedRow> insertToDb(TestDataSet dataset) throws SQLException {
        List<GeneratedRow> rows = dataset.generate();
        dbInserter.insert(dataset.getSchema(), dataset.getTableName(),
                rows, dataset.getColumns());
        return rows;
    }

    /**
     * Generates rows from {@code dataset} and writes them as Oracle SQL INSERT
     * statements to {@code outputPath}.
     *
     * @param dataset    configured test data set
     * @param outputPath destination file for the SQL script
     * @return the generated rows
     * @throws SQLException if column metadata access fails
     * @throws IOException  if the file cannot be written
     */
    public List<GeneratedRow> exportToSql(TestDataSet dataset, Path outputPath)
            throws SQLException, IOException {
        List<GeneratedRow> rows = dataset.generate();
        sqlScriptExporter.export(dataset.getSchema(), dataset.getTableName(),
                rows, dataset.getColumns(), outputPath);
        return rows;
    }

    /**
     * Generates rows from {@code dataset}, inserts them into the database, and
     * also writes them to a SQL script file. Both outputs receive the exact same rows.
     *
     * @param dataset    configured test data set
     * @param outputPath destination file for the SQL script
     * @return the generated rows
     * @throws SQLException if the INSERT or metadata access fails
     * @throws IOException  if the file cannot be written
     */
    public List<GeneratedRow> insertAndExport(TestDataSet dataset, Path outputPath)
            throws SQLException, IOException {
        List<GeneratedRow> rows = dataset.generate();
        dbInserter.insert(dataset.getSchema(), dataset.getTableName(),
                rows, dataset.getColumns());
        sqlScriptExporter.export(dataset.getSchema(), dataset.getTableName(),
                rows, dataset.getColumns(), outputPath);
        return rows;
    }
}
