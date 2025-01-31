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

package schemacrawler.loader.weakassociations;

import static schemacrawler.test.utility.ProposedWeakAssociationsTestUtility.weakAssociations;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

import schemacrawler.test.utility.DisableLogging;
import schemacrawler.test.utility.ResolveTestContext;
import schemacrawler.test.utility.TestContext;
import schemacrawler.test.utility.WithTestDatabase;

@DisableLogging
@ResolveTestContext
public class PrimaryKeyWeakAssociationsTest {

  @Test
  @WithTestDatabase(script = "/pk_test_1.sql")
  public void weakAssociations1(final TestContext testContext, final Connection connection)
      throws Exception {
    weakAssociations(testContext, connection, false);
    weakAssociations(testContext, connection, true);
  }

  @Test
  @WithTestDatabase(script = "/pk_test_2.sql")
  public void weakAssociations2(final TestContext testContext, final Connection connection)
      throws Exception {
    weakAssociations(testContext, connection, false);
    weakAssociations(testContext, connection, true);
  }

  @Test
  @WithTestDatabase(script = "/pk_test_3.sql")
  public void weakAssociations3(final TestContext testContext, final Connection connection)
      throws Exception {
    weakAssociations(testContext, connection, false);
    weakAssociations(testContext, connection, true);
  }
}
