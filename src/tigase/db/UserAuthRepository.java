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
package tigase.db;

import java.util.Map;
import java.security.NoSuchAlgorithmException;

/**
 * Describe interface UserAuthRepository here.
 *
 *
 * Created: Sun Nov  5 21:15:46 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface UserAuthRepository {

	void initRepository(String resource_uri) throws DBInitException;

	boolean plainAuth(String user, String password)
		throws UserNotFoundException, TigaseDBException;

	boolean digestAuth(String user, String digest, String id, String alg)
		throws UserNotFoundException, TigaseDBException, NoSuchAlgorithmException;

	boolean otherAuth(Map<String, Object> authProps)
		throws UserNotFoundException, TigaseDBException, NoSuchAlgorithmException;

  void addUser(String user, String password)
		throws UserExistsException, TigaseDBException;

  void updatePassword(String user, String password)
		throws UserExistsException, TigaseDBException;

  void removeUser(String user) throws UserNotFoundException, TigaseDBException;

} // UserAuthRepository
