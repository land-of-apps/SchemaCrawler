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

package schemacrawler.tools.commandline.command;


import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;
import schemacrawler.tools.commandline.state.BaseStateHolder;
import schemacrawler.tools.commandline.state.SchemaCrawlerShellState;
import schemacrawler.tools.commandline.utility.SchemaCrawlerOptionsConfig;
import schemacrawler.tools.commandline.utility.SchemaRetrievalOptionsConfig;
import schemacrawler.tools.databaseconnector.DatabaseConnectionOptions;
import schemacrawler.tools.databaseconnector.DatabaseConnectionSource;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.databaseconnector.DatabaseServerHostConnectionOptions;
import schemacrawler.tools.databaseconnector.DatabaseUrlConnectionOptions;
import schemacrawler.tools.databaseconnector.UserCredentials;
import schemacrawler.tools.options.Config;
import schemacrawler.SchemaCrawlerLogger;
import us.fatehi.utility.string.StringFormat;

@Command(name = "connect",
         header = "** Connect to the database",
         description = {
           "",
           "For database connections, please read",
           "https://www.schemacrawler.com/database-support.html",
           "first, before running SchemaCrawler",
           ""
         },
         headerHeading = "",
         synopsisHeading = "Shell Command:%n",
         customSynopsis = {
           "connect"
         },
         optionListHeading = "Options:%n")
public class ConnectCommand
  extends BaseStateHolder
  implements Runnable
{

  private static final SchemaCrawlerLogger LOGGER =
    SchemaCrawlerLogger.getLogger(ConnectCommand.class.getName());

  @ArgGroup(exclusive = true)
  private DatabaseConnectionGroupOptions databaseConnectionGroupOptions;
  @Spec
  private Model.CommandSpec spec;
  @Mixin
  private UserCredentialsOptions userCredentialsOptions;

  public ConnectCommand(final SchemaCrawlerShellState state)
  {
    super(state);
  }

  @Override
  public void run()
  {

    try
    {
      // Match the database connector in the best possible way, using the
      // server argument, or the JDBC connection URL
      final DatabaseConnectionOptions databaseConnectionOptions = getDatabaseConnectionOptions();
      requireNonNull(databaseConnectionOptions,
                     "No database connection options provided");
      final DatabaseConnector databaseConnector =
        databaseConnectionOptions.getDatabaseConnector();
      requireNonNull(databaseConnector,
                     "No database connection options provided");
      LOGGER.log(Level.INFO,
                 new StringFormat("Using database plugin <%s>",
                                  databaseConnector.getDatabaseServerType()));

      final Config config = new Config();
      config.putAll(state.getAdditionalConfiguration());
      config.putAll(state.getBaseConfiguration());

      state.sweep();

      state.addAdditionalConfiguration(config);
      loadSchemaCrawlerOptionsBuilder(databaseConnector);
      createDataSource(databaseConnector,
                       databaseConnectionOptions,
                       getUserCredentials());
      loadSchemaRetrievalOptionsBuilder(databaseConnector);

    }
    catch (final SchemaCrawlerException e)
    {
      throw new RuntimeException(e.getMessage(), e);
    }
    catch (final SQLException e)
    {
      throw new RuntimeException("Cannot connect to database", e);
    }
  }

  public DatabaseConnectionOptions getDatabaseConnectionOptions()
  {
    if (databaseConnectionGroupOptions == null)
    {
      throw new ParameterException(spec.commandLine(),
                                   "No database connection options provided");
    }

    final DatabaseConnectionOptions databaseConnectionOptions =
      databaseConnectionGroupOptions.getDatabaseConnectionOptions();
    if (databaseConnectionOptions == null)
    {
      throw new ParameterException(spec.commandLine(),
                                   "No database connection options provided");
    }

    return databaseConnectionOptions;
  }

  private UserCredentials getUserCredentials()
  {

    if (userCredentialsOptions == null)
    {
      throw new ParameterException(spec.commandLine(),
                                   "No database connection credentials provided");
    }
    final UserCredentials userCredentials =
      userCredentialsOptions.getUserCredentials();
    return userCredentials;
  }

  private void createDataSource(final DatabaseConnector databaseConnector,
      final DatabaseConnectionOptions connectionOptions,
      final UserCredentials userCredentials) throws SchemaCrawlerException
  {
    requireNonNull(databaseConnector,
        "No database plugin provided");
    requireNonNull(connectionOptions,
        "No database connection options provided");
    requireNonNull(userCredentials,
        "No database connection user credentials provided");

    LOGGER.log(Level.FINE, () -> "Creating data-source");

    // Connect using connection options provided from the command-line,
    // provided configuration, and bundled configuration
    final DatabaseConnectionSource databaseConnectionSource;
    if (connectionOptions instanceof DatabaseServerHostConnectionOptions)
    {
      final DatabaseServerHostConnectionOptions serverHostConnectionOptions =
          (DatabaseServerHostConnectionOptions) connectionOptions;
      databaseConnectionSource = databaseConnector.newDatabaseConnectionSource(
          serverHostConnectionOptions.getHost(),
          serverHostConnectionOptions.getPort(),
          serverHostConnectionOptions.getDatabase(),
          serverHostConnectionOptions.getUrlx());
    } else if (connectionOptions instanceof DatabaseUrlConnectionOptions)
    {
      databaseConnectionSource = databaseConnector.newDatabaseConnectionSource(
          ((DatabaseUrlConnectionOptions) connectionOptions)
              .getConnectionUrl());
    } else
    {
      throw new SchemaCrawlerException("Database connection options not provided");
    }

    databaseConnectionSource.setUserCredentials(userCredentials);

    state.setDataSource(databaseConnectionSource);
  }

  private void loadSchemaCrawlerOptionsBuilder(
      final DatabaseConnector databaseConnector)
  {
    LOGGER.log(Level.FINE, () -> "Creating SchemaCrawler options builder");

    final Config config = state.getAdditionalConfiguration();
    final SchemaCrawlerOptionsBuilder schemaCrawlerOptionsBuilder =
        SchemaCrawlerOptionsBuilder.builder();
    // Set defaults from database plugins, such as default schema excludes
    databaseConnector
        .setDefaultsForSchemaCrawlerOptionsBuilder(schemaCrawlerOptionsBuilder);
    // Override with options from config file
    SchemaCrawlerOptionsConfig.fromConfig(schemaCrawlerOptionsBuilder, config);
    state.setSchemaCrawlerOptionsBuilder(schemaCrawlerOptionsBuilder);
  }

  private void loadSchemaRetrievalOptionsBuilder(final DatabaseConnector databaseConnector)
    throws SQLException
  {
    requireNonNull(databaseConnector,
                   "No database connection options provided");

    LOGGER.log(Level.FINE,
               () -> "Creating SchemaCrawler retrieval options builder");

    final Config config = state.getAdditionalConfiguration();
    try (
      final Connection connection = state
        .getDataSource()
        .get()
    )
    {
      final SchemaRetrievalOptionsBuilder schemaRetrievalOptionsBuilder =
        databaseConnector.getSchemaRetrievalOptionsBuilder(connection);
      state.setSchemaRetrievalOptionsBuilder(SchemaRetrievalOptionsConfig.fromConfig(schemaRetrievalOptionsBuilder, config));
    }
  }

}
