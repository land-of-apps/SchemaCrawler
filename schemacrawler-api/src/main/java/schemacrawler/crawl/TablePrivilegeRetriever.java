/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2022, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.crawl;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import schemacrawler.schema.Column;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

/** A retriever uses database metadata to get the extended details about the database tables. */
final class TablePrivilegeRetriever extends AbstractRetriever {

  private static final Logger LOGGER = Logger.getLogger(TablePrivilegeRetriever.class.getName());

  TablePrivilegeRetriever(
      final RetrieverConnection retrieverConnection,
      final MutableCatalog catalog,
      final SchemaCrawlerOptions options)
      throws SQLException {
    super(retrieverConnection, catalog, options);
  }

  void retrieveTableColumnPrivileges() throws SQLException {
    try (final MetadataResultSet results =
        new MetadataResultSet(
            getMetaData().getColumnPrivileges(null, null, null, null),
            "DatabaseMetaData::getColumnPrivileges")) {
      createPrivileges(results, true);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Could not retrieve table column privileges:" + e.getMessage());
    }
  }

  void retrieveTablePrivileges() throws SQLException {
    try (final MetadataResultSet results =
        new MetadataResultSet(
            getMetaData().getTablePrivileges(null, null, null),
            "DatabaseMetaData::getTablePrivileges")) {
      createPrivileges(results, false);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Could not retrieve table privileges", e);
    }
  }

  private void createPrivileges(final MetadataResultSet results, final boolean privilegesForColumn)
      throws SQLException {
    while (results.next()) {
      final String catalogName = normalizeCatalogName(results.getString("TABLE_CAT"));
      final String schemaName = normalizeSchemaName(results.getString("TABLE_SCHEM"));
      final String tableName = results.getString("TABLE_NAME");
      final String columnName;
      if (privilegesForColumn) {
        columnName = results.getString("COLUMN_NAME");
      } else {
        columnName = null;
      }

      final Optional<MutableTable> tableOptional = lookupTable(catalogName, schemaName, tableName);
      if (!tableOptional.isPresent()) {
        continue;
      }

      final MutableTable table = tableOptional.get();
      final MutableColumn column;
      if (privilegesForColumn) {
        final Optional<MutableColumn> columnOptional = table.lookupColumn(columnName);
        if (!columnOptional.isPresent()) {
          continue;
        }
        column = columnOptional.get();
      } else {
        column = null;
      }

      final String privilegeName = results.getString("PRIVILEGE");
      final String grantor = results.getString("GRANTOR");
      final String grantee = results.getString("GRANTEE");
      final boolean isGrantable = results.getBoolean("IS_GRANTABLE");

      final MutablePrivilege<?> privilege;
      if (privilegesForColumn) {
        final Optional<MutablePrivilege<Column>> privilegeOptional =
            column.lookupPrivilege(privilegeName);
        privilege =
            privilegeOptional.orElse(
                new MutablePrivilege<>(new ColumnPointer(column), privilegeName));
      } else {
        final Optional<MutablePrivilege<Table>> privilegeOptional =
            table.lookupPrivilege(privilegeName);
        privilege =
            privilegeOptional.orElse(
                new MutablePrivilege<>(new TablePointer(table), privilegeName));
      }

      privilege.addGrant(grantor, grantee, isGrantable);

      if (privilegesForColumn) {
        column.addPrivilege((MutablePrivilege<Column>) privilege);
      } else {
        table.addPrivilege((MutablePrivilege<Table>) privilege);
      }
    }
  }
}
