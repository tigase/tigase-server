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
package tigase.net;

import java.net.InetAddress;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;

/**
 * Describe class AllAddresses here.
 *
 *
 * Created: Mon Sep 11 09:59:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AllAddresses {

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {

		String host = "gmail.com";
		if (args.length > 0) {
			host = args[0];
		} // end of if (args.length > 1)

		InetAddress[] all = InetAddress.getAllByName(host);
		for (InetAddress ia: all) {
			System.out.println("Host: " + ia.toString());
		} // end of for (InetAddress ia: all)

		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		//		env.put("java.naming.provider.url", "dns://10.75.32.10");
		DirContext ctx = new InitialDirContext(env);
		Attributes attrs = ctx.getAttributes("_xmpp-server._tcp." + host,
			new String[] {"SRV"});
		for (NamingEnumeration ae = attrs.getAll();ae.hasMoreElements();) {
			Attribute attr = (Attribute)ae.next();
			String attrId = attr.getID();
			for (Enumeration vals = attr.getAll(); vals.hasMoreElements();
					 System.out.println(attrId + ": " + vals.nextElement()));
		}
		ctx.close();
	}


} // AllAddresses
