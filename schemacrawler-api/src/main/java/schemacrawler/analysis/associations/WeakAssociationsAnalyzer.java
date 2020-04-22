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
package schemacrawler.analysis.associations;


import static java.util.Objects.requireNonNull;
import static schemacrawler.analysis.associations.WeakAssociationsUtility.addWeakAssociationToTable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import schemacrawler.schema.Column;
import schemacrawler.schema.Table;
import schemacrawler.utility.MetaDataUtility;
import sf.util.SchemaCrawlerLogger;
import sf.util.StringFormat;

final class WeakAssociationsAnalyzer
{

  private static final SchemaCrawlerLogger LOGGER =
    SchemaCrawlerLogger.getLogger(WeakAssociationsAnalyzer.class.getName());

  private final List<Table> tables;
  private final Collection<WeakAssociationForeignKey> weakAssociations;

  WeakAssociationsAnalyzer(final List<Table> tables)
  {
    this.tables = requireNonNull(tables, "No tables provided");
    weakAssociations = new TreeSet<>();
  }

  Collection<WeakAssociationForeignKey> analyzeTables()
  {
    if (tables.size() < 2)
    {
      return Collections.emptySet();
    }

    findWeakAssociations(tables);

    return weakAssociations;
  }

  private void addWeakAssociation(final WeakAssociation weakAssociation)
  {
    final String weakFkName = MetaDataUtility.constructForeignKeyName(
      weakAssociation.getPrimaryKeyColumn(),
      weakAssociation.getForeignKeyColumn());
    final WeakAssociationForeignKey weakFk =
      new WeakAssociationForeignKey(weakFkName);
    weakFk.add(weakAssociation);

    weakAssociations.add(weakFk);

    addWeakAssociationToTable(weakAssociation
                                .getPrimaryKeyColumn()
                                .getParent(), weakFk);
    addWeakAssociationToTable(weakAssociation
                                .getForeignKeyColumn()
                                .getParent(), weakFk);
  }

  private void findWeakAssociations(final List<Table> tables)
  {
    LOGGER.log(Level.INFO, "Finding weak associations");
    final ForeignKeys foreignKeys = new ForeignKeys(tables);
    final ColumnMatchKeysMap columnMatchKeysMap =
      new ColumnMatchKeysMap(tables);
    final TableMatchKeys tableMatchKeys = new TableMatchKeys(tables);

    if (LOGGER.isLoggable(Level.FINER))
    {
      LOGGER.log(Level.FINER,
                 new StringFormat("Column match keys <%s>",
                                  columnMatchKeysMap));
      LOGGER.log(Level.FINER,
                 new StringFormat("Column match keys <%s>", tableMatchKeys));
    }
    for (final Table table : tables)
    {
      final TableCandidateKeys tableCandidateKeys =
        new TableCandidateKeys(table);
      LOGGER.log(Level.FINER,
                 new StringFormat("Table candidate keys <%s>",
                                  tableCandidateKeys));
      for (final Column pkColumn : tableCandidateKeys)
      {
        final Set<String> fkColumnMatchKeys = new HashSet<>();
        // Look for all columns matching this table match key
        if (pkColumn.isPartOfPrimaryKey())
        {
          fkColumnMatchKeys.addAll(tableMatchKeys.get(table));
        }
        // Look for all columns matching this column match key
        if (columnMatchKeysMap.containsKey(pkColumn))
        {
          fkColumnMatchKeys.addAll(columnMatchKeysMap.get(pkColumn));
        }

        final Set<Column> fkColumns = new HashSet<>();
        for (final String fkColumnMatchKey : fkColumnMatchKeys)
        {
          if (columnMatchKeysMap.containsKey(fkColumnMatchKey))
          {
            fkColumns.addAll(columnMatchKeysMap.get(fkColumnMatchKey));
          }
        }

        for (final Column fkColumn : fkColumns)
        {
          if (pkColumn.equals(fkColumn))
          {
            continue;
          }

          final ProposedWeakAssociation proposedWeakAssociation =
            new ProposedWeakAssociation(pkColumn, fkColumn);
          if (proposedWeakAssociation.isValid()
              && !foreignKeys.contains(proposedWeakAssociation))
          {
            LOGGER.log(Level.FINE,
                       new StringFormat("Found weak association <%s>",
                                        proposedWeakAssociation));
            final WeakAssociation weakAssociation =
              new WeakAssociation(proposedWeakAssociation.getKey(), proposedWeakAssociation.getValue());
            addWeakAssociation(weakAssociation);
          }
        }
      }
    }
  }

}
