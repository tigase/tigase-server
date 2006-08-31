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
import java.util.logging.Logger;

/**
 * Describe class DialbackAuth here.
 *
 *
 * Created: Mon Aug 28 11:45:17 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DialbackAuth extends PlainAuth {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.auth.DigestAuth");

	protected boolean passwordsEqual(final String given_password,
		final String db_password) throws NoSuchAlgorithmException {
		return true;
	}

} // DialbackAuth
