/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.auth;

import java.security.NoSuchAlgorithmException;
import tigase.util.Algorithms;

/**
 * Describe class DigestAuth here.
 *
 *
 * Created: Sat Feb 18 13:54:21 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DigestAuth extends PlainAuth {

	protected boolean passwordsEqual(final String given_password,
		final String db_password) throws NoSuchAlgorithmException {
		final String digest_db_pass =
			Algorithms.digest(connection.getSessionId(), db_password, "SHA");
		return given_password.equals(digest_db_pass);
	}


} // DigestAuth
