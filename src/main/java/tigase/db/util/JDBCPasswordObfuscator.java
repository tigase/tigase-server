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

import tigase.db.DataRepository;
import tigase.db.jdbc.DataRepositoryImpl;

public class JDBCPasswordObfuscator {

	private static String getObfuscatedUrl(String url, char separatorCharacter) {
		String passwordParameter = "password=";
		final int passwordIndex = url.lastIndexOf(passwordParameter);
		final String urlReminder = url.substring(passwordIndex + passwordParameter.length());
		int nextSectionIndex = urlReminder.indexOf(separatorCharacter);
		int passwordLength = nextSectionIndex > 0 ? nextSectionIndex : urlReminder.length();
		url = url.substring(0, passwordIndex + passwordParameter.length()) + "*".repeat(passwordLength) +
				urlReminder.substring(passwordLength);
		return url;
	}

	public static String obfuscatePassword(String url) {

		final DataRepository.dbTypes dbType = DataRepositoryImpl.parseDatabaseType(url);

		switch (dbType) {
			case postgresql:
			case mysql:
				url = getObfuscatedUrl(url, '&');
				break;
			case jtds:
			case sqlserver:
				url = getObfuscatedUrl(url, ';');
				break;
		}

		return url;
	}
}
