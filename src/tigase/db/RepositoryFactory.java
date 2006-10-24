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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Describe class RepositoryFactory here.
 *
 *
 * Created: Tue Oct 24 22:13:52 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RepositoryFactory {

	private static ConcurrentMap<String, UserRepository> instances =
		new ConcurrentHashMap<String, UserRepository>();

	public static UserRepository getInstance(String class_name, String resource)
		throws ClassNotFoundException, InstantiationException,
					 IllegalAccessException {
		UserRepository rep = instances.get(resource);
		if (rep == null) {
			rep = (UserRepository)Class.forName(class_name).newInstance();
			rep.initRepository(resource);
			instances.put(resource, rep);
		} // end of if (rep == null)
		return rep;
	}



} // RepositoryFactory
