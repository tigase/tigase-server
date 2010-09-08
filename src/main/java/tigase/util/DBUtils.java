/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.util;

//~--- non-JDK imports --------------------------------------------------------

import static tigase.conf.Configurable.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class DBUtils here.
 *
 *
 * Created: Thu Nov  8 08:59:06 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class DBUtils {

	/**
	 * Method description
	 *
	 *
	 * @param params
	 * @param primaryKey
	 * @param secondaryKey
	 *
	 * @return
	 */
	public static String[] decodeDBParams(final Map<String, Object> params,
			final String primaryKey, final String secondaryKey) {
		String repo_class = null;
		String repo_url = null;
		String repo = (String) params.get(primaryKey);

		if ((repo == null) && (secondaryKey != null)) {
			repo = (String) params.get(secondaryKey);
		}

		if (repo != null) {
			if (repo.equals("mysql")) {
				repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				repo_url = MYSQL_REPO_URL_PROP_VAL;
			} else {
				if (repo.equals("pgsql")) {
					repo_class = PGSQL_REPO_CLASS_PROP_VAL;
					repo_url = PGSQL_REPO_URL_PROP_VAL;
				} else {
					if (repo.equals("drupal")) {
						repo_class = DRUPALWP_REPO_CLASS_PROP_VAL;
						repo_url = DRUPAL_REPO_URL_PROP_VAL;
					} else {
						if (repo.equals("libresource")) {
							repo_class = LIBRESOURCE_REPO_CLASS_PROP_VAL;
							repo_url = LIBRESOURCE_REPO_URL_PROP_VAL;
						} else {
							repo_class = repo;
						}
					}
				}
			}
		}

		return new String[] { repo_class, repo_url };
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
