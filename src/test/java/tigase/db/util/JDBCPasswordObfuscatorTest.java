/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.db.util;

import org.junit.Assert;
import org.junit.Test;

public class JDBCPasswordObfuscatorTest {

	@Test
	public void obfuscateMysqlPassword() {
		String password = "tigase_password";
		final String input = "jdbc:mysql://localhost/tigasedb?user=tigasedb&password=" + password + "&useSSL=false";
		final String obfuscated = JDBCPasswordObfuscator.obfuscatePassword(input);
		System.out.println(input);
		System.out.println(obfuscated);
		Assert.assertEquals("jdbc:mysql://localhost/tigasedb?user=tigasedb&password=***************&useSSL=false", obfuscated);
	}

	@Test
	public void obfuscatePostgresqlPassword() {
		String password = "tigase_password";
		final String input = "jdbc:postgresql://localhost/tigasedb?user=tigasedb&password=" + password + "&useSSL=false";
		final String obfuscated = JDBCPasswordObfuscator.obfuscatePassword(input);
		Assert.assertEquals( "jdbc:postgresql://localhost/tigasedb?user=tigasedb&password=" + "*".repeat(password.length()) + "&useSSL=false", obfuscated);
	}

	@Test
	public void obfuscateSqlserverSecurePassword() {
		String password = "1Secure*Password1";
		final String input = "jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=" + password + ";schema=dbo;lastUpdateCount=false";
		final String obfuscated = JDBCPasswordObfuscator.obfuscatePassword(input);
		Assert.assertEquals("jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=*****************;schema=dbo;lastUpdateCount=false", obfuscated);
	}
	@Test
	public void obfuscateSqlserverPassword() {
		String password = "tigase12";
		final String input = "jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=" + password + ";schema=dbo;lastUpdateCount=false";
		final String obfuscated = JDBCPasswordObfuscator.obfuscatePassword(input);
		Assert.assertEquals("jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=********;schema=dbo;lastUpdateCount=false", obfuscated);
	}
	@Test
	public void obfuscateJtdsSqlserverPassword() {
		String password = "tigase12";
		final String input = "jdbc:jtds:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=" + password + ";schema=dbo;lastUpdateCount=false";
		final String obfuscated = JDBCPasswordObfuscator.obfuscatePassword(input);
		Assert.assertEquals("jdbc:jtds:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=********;schema=dbo;lastUpdateCount=false", obfuscated);
	}
}