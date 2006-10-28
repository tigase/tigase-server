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
package tigase.util;

import java.util.List;
import tigase.db.UserRepository;
import tigase.db.RepositoryFactory;
import tigase.db.UserExistsException;

/**
 * Describe class RepositoryUtils here.
 *
 *
 * Created: Sat Oct 28 13:09:26 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RepositoryUtils {

	public static void printNode(String user, UserRepository repo,
		String prefix, String node)	throws Exception {
		System.out.println(prefix + "node: " + node);
		String[] keys = repo.getKeys(user, node);
		if (keys != null) {
			for (String key: keys) {
				String[] vals = repo.getDataList(user, node, key);
				if (vals != null) {
					String valstr = "";
					for (String val: vals) {
						valstr = valstr + " " + val;
					} // end of for (String val: vals)
					System.out.println(prefix + "  " + key + " = " + valstr);
				} else {
					System.out.println("    " + key);
				} // end of if (vals != null) else
			} // end of for (String key: keys)
		} // end of if (keys != null)
		String[] nodes = repo.getSubnodes(user, node);
		if (nodes != null) {
			for (String subnode: nodes) {
				printNode(user, repo, prefix + "  ", node + "/" + subnode);
			} // end of for (String node: nodes)
		} // end of if (ndoes != null)
	}

	public static void printRepoContent(UserRepository repo)
		throws Exception {
		List<String> users = repo.getUsers();
		if (users != null) {
			for (String user: users) {
				System.out.println(user);
				String[] nodes = repo.getSubnodes(user, null);
				if (nodes != null) {
					for (String node: nodes) {
						printNode(user, repo, "  ", node);
					} // end of for (String node: nodes)
				} // end of if (ndoes != null)
			} // end of for (String user: users)
		} else {
			System.out.println("There are no user accounts in repository.");
		} // end of else
	}

	private static String user1 = "user111@hostname";
	private static String user2 = "user222@hostname";
	private static String user3 = "user333@hostname";

	public static void removeTestData(UserRepository repo) throws Exception {
		repo.removeUser(user1);
		repo.removeUser(user2);
		repo.removeUser(user3);
	}

	public static void loadTestData(UserRepository repo) throws Exception {
		try { repo.addUser(user1); } catch (UserExistsException e) {	}
		try { repo.addUser(user2); } catch (UserExistsException e) {	}
		try { repo.addUser(user3); } catch (UserExistsException e) {	}
		repo.setData(user1, null, "password", "secret111");
		repo.setData(user2, null, "password", "secret222");
		repo.setData(user3, null, "password", "secret333");
		repo.setData(user1, "roster/buddy111", "name", "budy1");
		repo.setData(user1, "roster/buddy222", "name", "budy2");
		repo.setData(user1, "roster/buddy333", "name", "budy3");
		repo.setDataList(user1, "roster/buddy111", "groups",
			new String[] {"buddies", "friends"});
		repo.setDataList(user2, "roster/buddy111", "groups",
			new String[] {"buddies", "friends"});
		repo.addDataList(user2, "roster/buddy111", "groups",
			new String[] {"family", "home"});
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {
		String repo_url =
			"jdbc:mysql://localhost/tigase?user=root&password=admin12";
		String repo_class = "tigase.db.jdbc.JDBCRepository";
		UserRepository repo =
			RepositoryFactory.getInstance(repo_class, repo_url);
		printRepoContent(repo);
		loadTestData(repo);
		printRepoContent(repo);
		removeTestData(repo);
		printRepoContent(repo);
	}

} // RepositoryUtils
