/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2020, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static schemacrawler.analysis.counts.TableRowCountsUtility.getRowCountMessage;
import static schemacrawler.analysis.counts.TableRowCountsUtility.hasRowCount;
import static schemacrawler.test.utility.DatabaseTestUtility.getCatalog;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAs;
import static schemacrawler.test.utility.FileHasContent.outputOf;

import java.sql.Connection;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.Routine;
import schemacrawler.schema.RoutineParameter;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.FilterOptionsBuilder;
import schemacrawler.schemacrawler.GrepOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.test.utility.TestContext;
import schemacrawler.test.utility.TestContextParameterResolver;
import schemacrawler.test.utility.TestDatabaseConnectionParameterResolver;
import schemacrawler.test.utility.TestWriter;

@ExtendWith(TestDatabaseConnectionParameterResolver.class)
@ExtendWith(TestContextParameterResolver.class)
public class SchemaCrawlerGrepTest
{

  @Test
  public void grepColumns(final TestContext testContext,
                          final Connection connection)
    throws Exception
  {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout)
    {
      final GrepOptionsBuilder grepOptionsBuilder = GrepOptionsBuilder.builder()
        .includeGreppedColumns(new RegularExpressionInclusionRule(
          ".*\\..*\\.BOOKID"));
      final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder
          .builder()
          .withGrepOptions(grepOptionsBuilder.toOptions())
          .toOptions();

      final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);
      final Schema[] schemas = catalog
        .getSchemas()
        .toArray(new Schema[0]);
      assertThat("Schema count does not match", schemas, arrayWithSize(6));
      for (final Schema schema : schemas)
      {
        out.println("schema: " + schema.getFullName());
        final Table[] tables = catalog
          .getTables(schema)
          .toArray(new Table[0]);
        for (final Table table : tables)
        {
          out.println("  table: " + table.getFullName());
          final Column[] columns = table
            .getColumns()
            .toArray(new Column[0]);
          Arrays.sort(columns);
          for (final Column column : columns)
          {
            out.println("    column: " + column.getFullName());
          }
        }
      }
    }
    assertThat(outputOf(testout),
               hasSameContentAs(classpathResource(testContext.testMethodFullName())));
  }

  @Test
  public void grepColumnsAndIncludeParentTables(final Connection connection)
    throws Exception
  {
    final GrepOptionsBuilder grepOptionsBuilder = GrepOptionsBuilder.builder()
      .includeGreppedColumns(new RegularExpressionInclusionRule(
        ".*\\.BOOKAUTHORS\\..*"));
    final FilterOptionsBuilder filterOptionsBuilder = FilterOptionsBuilder.builder()
      .parentTableFilterDepth(1);
    SchemaCrawlerOptions schemaCrawlerOptions = SchemaCrawlerOptionsBuilder
      .builder()
      .withGrepOptions(grepOptionsBuilder.toOptions())
      .toOptions();

    Catalog catalog;
    Schema schema;
    Table table;

    catalog = getCatalog(connection, schemaCrawlerOptions);
    schema = catalog
      .lookupSchema("PUBLIC.BOOKS")
      .get();
    assertThat("Schema PUBLIC.BOOKS not found", schema, notNullValue());
    assertThat(catalog.getTables(schema), hasSize(1));
    table = catalog
      .lookupTable(schema, "BOOKAUTHORS")
      .get();
    assertThat("Table BOOKAUTHORS not found", table, notNullValue());

    schemaCrawlerOptions = SchemaCrawlerOptionsBuilder
      .builder()
      .fromOptions(schemaCrawlerOptions)
      .withFilterOptionsBuilder(filterOptionsBuilder)
      .toOptions();
    catalog = getCatalog(connection, schemaCrawlerOptions);
    schema = catalog
      .lookupSchema("PUBLIC.BOOKS")
      .get();
    assertThat("Schema PUBLIC.BOOKS not found", schema, notNullValue());
    assertThat(catalog
                 .getTables(schema)
                 .size(), is(3));
    table = catalog
      .lookupTable(schema, "BOOKAUTHORS")
      .get();
    assertThat("Table BOOKAUTHORS not found", table, notNullValue());
    table = catalog
      .lookupTable(schema, "BOOKS")
      .get();
    assertThat("Table BOOKS not found", table, notNullValue());
    table = catalog
      .lookupTable(schema, "AUTHORS")
      .get();
    assertThat("Table AUTHORS not found", table, notNullValue());

  }

  @Test
  public void grepCombined(final TestContext testContext,
                           final Connection connection)
    throws Exception
  {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout)
    {
      final GrepOptionsBuilder grepOptionsBuilder = GrepOptionsBuilder.builder()
        .includeGreppedColumns(new RegularExpressionInclusionRule(
          ".*\\..*\\.BOOKID"))
        .includeGreppedDefinitions(new RegularExpressionInclusionRule(
          ".*book author.*"));
      final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder
          .builder()
          .withGrepOptions(grepOptionsBuilder.toOptions())
          .toOptions();

      final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);
      final Schema[] schemas = catalog
        .getSchemas()
        .toArray(new Schema[0]);
      assertThat("Schema count does not match", schemas, arrayWithSize(6));
      for (final Schema schema : schemas)
      {
        out.println("schema: " + schema.getFullName());
        final Table[] tables = catalog
          .getTables(schema)
          .toArray(new Table[0]);
        for (final Table table : tables)
        {
          out.println("  table: " + table.getFullName());
          final Column[] columns = table
            .getColumns()
            .toArray(new Column[0]);
          Arrays.sort(columns);
          for (final Column column : columns)
          {
            out.println("    column: " + column.getFullName());
          }
        }
      }
    }
    assertThat(outputOf(testout),
               hasSameContentAs(classpathResource(testContext.testMethodFullName())));
  }

  @Test
  public void grepDefinitions(final TestContext testContext,
                              final Connection connection)
    throws Exception
  {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout)
    {
      final GrepOptionsBuilder grepOptionsBuilder = GrepOptionsBuilder.builder()
        .includeGreppedDefinitions(new RegularExpressionInclusionRule(
          ".*book author.*"));
      final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder
          .builder()
          .withGrepOptions(grepOptionsBuilder.toOptions())
          .toOptions();

      final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);
      final Schema[] schemas = catalog
        .getSchemas()
        .toArray(new Schema[0]);
      assertThat("Schema count does not match", schemas, arrayWithSize(6));
      for (final Schema schema : schemas)
      {
        out.println("schema: " + schema.getFullName());
        final Table[] tables = catalog
          .getTables(schema)
          .toArray(new Table[0]);
        for (final Table table : tables)
        {
          out.println("  table: " + table.getFullName());
          final Column[] columns = table
            .getColumns()
            .toArray(new Column[0]);
          Arrays.sort(columns);
          for (final Column column : columns)
          {
            out.println("    column: " + column.getFullName());
          }
        }
      }
    }
    assertThat(outputOf(testout),
               hasSameContentAs(classpathResource(testContext.testMethodFullName())));
  }

  @Test
  public void grepProcedures(final TestContext testContext,
                             final Connection connection)
    throws Exception
  {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout)
    {
      final GrepOptionsBuilder grepOptionsBuilder = GrepOptionsBuilder.builder()
        .includeGreppedRoutineParameters(new RegularExpressionInclusionRule(
          ".*\\.B_COUNT"));
      final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder
          .builder()
          .includeAllRoutines()
          .withGrepOptions(grepOptionsBuilder.toOptions())
          .toOptions();

      final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);
      final Schema[] schemas = catalog
        .getSchemas()
        .toArray(new Schema[0]);
      assertThat("Schema count does not match", schemas, arrayWithSize(6));
      for (final Schema schema : schemas)
      {
        out.println("schema: " + schema.getFullName());
        final Routine[] routines = catalog
          .getRoutines(schema)
          .toArray(new Routine[0]);
        for (final Routine routine : routines)
        {
          out.println("  routine: " + routine.getFullName());
          final RoutineParameter[] parameters = routine
            .getParameters()
            .toArray(new RoutineParameter[0]);
          for (final RoutineParameter column : parameters)
          {
            out.println("    parameter: " + column.getFullName());
          }
        }
      }
    }
    assertThat(outputOf(testout),
               hasSameContentAs(classpathResource(testContext.testMethodFullName())));

  }

  @Test
  public void noEmptyTables(final TestContext testContext,
                            final Connection connection)
    throws Exception
  {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout)
    {
      final FilterOptionsBuilder filterOptionsBuilder = FilterOptionsBuilder.builder()
        .noEmptyTables();
      final LoadOptionsBuilder loadOptionsBuilder = LoadOptionsBuilder.builder()
        .loadRowCounts();
      final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder
          .builder()
          .withFilterOptionsBuilder(filterOptionsBuilder)
          .withLoadOptionsBuilder(loadOptionsBuilder)
          .toOptions();

      final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);
      final Schema[] schemas = catalog
        .getSchemas()
        .toArray(new Schema[0]);
      assertThat("Schema count does not match", schemas, arrayWithSize(6));
      for (final Schema schema : schemas)
      {
        final Table[] tables = catalog
          .getTables(schema)
          .toArray(new Table[0]);
        for (final Table table : tables)
        {
          assertThat(hasRowCount(table), is(true));
          out.println(String.format("%s [%s]",
                                    table.getFullName(),
                                    getRowCountMessage(table)));
        }
      }
    }
    assertThat(outputOf(testout),
               hasSameContentAs(classpathResource(testContext.testMethodFullName())));
  }

}
