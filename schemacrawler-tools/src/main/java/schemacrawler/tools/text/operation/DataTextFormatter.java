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

package schemacrawler.tools.text.operation;


import static schemacrawler.analysis.counts.TableRowCountsUtility.getRowCountMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import schemacrawler.crawl.MetadataResultSet;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.Query;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.tools.options.OutputOptions;
import schemacrawler.tools.text.base.BaseTabularFormatter;
import schemacrawler.tools.text.utility.TextFormattingHelper.DocumentHeaderType;
import schemacrawler.tools.text.utility.html.Alignment;
import schemacrawler.tools.traversal.DataTraversalHandler;
import sf.util.Color;

/**
 * Text formatting of data.
 *
 * @author Sualeh Fatehi
 */
final class DataTextFormatter
  extends BaseTabularFormatter<OperationOptions>
  implements DataTraversalHandler
{

  private static String getMessage(final double aggregate)
  {
    final Number number;
    if (Math.abs(aggregate - (int) aggregate) < 1E-10D)
    {
      number = Integer.valueOf((int) aggregate);
    }
    else
    {
      number = Double.valueOf(aggregate);
    }
    final String message = getRowCountMessage(number);
    return message;
  }

  private final Operation operation;
  private int dataBlockCount;

  /**
   * Text formatting of data.
   *
   * @param operation
   *   Options for text formatting of data
   * @param options
   *   Options for text formatting of data
   * @param outputOptions
   *   Options for text formatting of data
   * @param identifierQuoteString
   *   Quote character for identifier
   */
  DataTextFormatter(final Operation operation,
                    final OperationOptions options,
                    final OutputOptions outputOptions,
                    final String identifierQuoteString)
    throws SchemaCrawlerException
  {
    super(options,
      /* printVerboseDatabaseInfo */
          false, outputOptions, identifierQuoteString);
    this.operation = operation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end()
    throws SchemaCrawlerException
  {
    if (operation == Operation.count)
    {
      formattingHelper.writeObjectEnd();
    }

    super.end();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleData(final Query query, final ResultSet rows)
    throws SchemaCrawlerException
  {
    final String title;
    if (query != null)
    {
      title = query.getName();
    }
    else
    {
      title = "";
    }

    handleData(title, rows);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleData(final Table table, final ResultSet rows)
    throws SchemaCrawlerException
  {
    final String tableName;
    if (table != null)
    {
      if (options.isShowUnqualifiedNames())
      {
        tableName = identifiers.quoteName(table);
      }
      else
      {
        tableName = identifiers.quoteFullName(table);
      }
    }
    else
    {
      tableName = "";
    }

    handleData(tableName, rows);
  }

  /**
   * Handles an aggregate operation, such as a count, for a given table.
   *
   * @param title
   *   Title
   * @param results
   *   Results
   */
  private void handleAggregateOperationForTable(final String title,
                                                final ResultSet results)
    throws SchemaCrawlerException
  {
    long aggregate = 0;
    try
    {
      if (results.next())
      {
        aggregate = results.getLong(1);
      }
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerException("Could not obtain aggregate data", e);
    }
    final String message = getMessage(aggregate);
    //
    formattingHelper.writeNameValueRow(title, message, Alignment.right);
  }

  private void handleData(final String title, final ResultSet rows)
    throws SchemaCrawlerException
  {
    if (rows == null)
    {
      return;
    }

    if (dataBlockCount == 0)
    {
      printHeader();
    }

    if (operation == Operation.count)
    {
      handleAggregateOperationForTable(title, rows);
    }
    else
    {
      formattingHelper.println();
      formattingHelper.println();
      formattingHelper.writeObjectStart();
      formattingHelper.writeObjectNameRow("", title, "", Color.white);
      try
      {
        final MetadataResultSet dataRows = new MetadataResultSet(rows);
        dataRows.setShowLobs(options.isShowLobs());

        formattingHelper.writeRowHeader(dataRows.getColumnNames());

        iterateRows(dataRows);
      }
      catch (final SQLException e)
      {
        throw new SchemaCrawlerException(e.getMessage(), e);
      }
      formattingHelper.writeObjectEnd();
    }

    dataBlockCount++;
  }

  private void iterateRows(final MetadataResultSet dataRows)
    throws SQLException
  {
    while (dataRows.next())
    {
      final List<Object> currentRow = dataRows.row();
      final Object[] columnData =
        currentRow.toArray(new Object[currentRow.size()]);
      formattingHelper.writeRow(columnData);
    }
  }

  private void printHeader()
  {
    if (operation == null)
    {
      // If there is no operation, assume that the command is a query
      formattingHelper.writeHeader(DocumentHeaderType.subTitle, "Query");
      return;
    }

    formattingHelper.writeHeader(DocumentHeaderType.subTitle,
                                 operation.getTitle());

    if (operation == Operation.count)
    {
      formattingHelper.writeObjectStart();
      formattingHelper.writeObjectNameRow("",
                                          operation.getTitle(),
                                          "",
                                          Color.white);
    }
  }
}
