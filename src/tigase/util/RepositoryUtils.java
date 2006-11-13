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
import tigase.db.RepositoryFactory;
import tigase.db.UserAuthRepository;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

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

	public static void copyNode(String user, String node,
		UserRepository src, UserRepository dst) throws Exception {
		String[] keys = src.getKeys(user, node);
		if (keys != null) {
			for (String key: keys) {
				String[] vals = src.getDataList(user, node, key);
				if (vals != null) {
					dst.setDataList(user, node, key, vals);
				} // end of if (vals != null) else
			} // end of for (String key: keys)
		} // end of if (keys != null)
		String[] nodes = src.getSubnodes(user, node);
		if (nodes != null) {
			for (String subnode: nodes) {
				copyNode(user,
					(node != null ? node + "/" + subnode : subnode),
					src, dst);
			} // end of for (String node: nodes)
		} // end of if (ndoes != null)
	}

	public static void copyUser(String user, UserRepository src, UserRepository dst)
		throws Exception {
		System.out.print("Copying user: " + user + "...");
		try {
			dst.addUser(user);
			copyNode(user, null, src, dst);
			System.out.println("OK");
		} catch (UserExistsException e) {
			System.out.println("ERROR, user already exists.");
		}
	}

	public static void copyUser(String user, UserRepository src,
		UserAuthRepository dst)
		throws Exception {
		System.out.print("Copying user: " + user + "...");
		String password = src.getData(user, "password");
		try {
			dst.addUser(user, password);
			System.out.println("OK");
		} catch (UserExistsException e) {
			System.out.println("ERROR, user already exists.");
		}
	}

	public static void printNode(String user, UserRepository repo,
		String prefix, String node)	throws Exception {
		if (node != null) {
			System.out.println(prefix + "node: " + node);
		} // end of if (node != null)
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
				printNode(user, repo, prefix + "  ",
					(node != null ? node + "/" + subnode : subnode));
			} // end of for (String node: nodes)
		} // end of if (ndoes != null)
	}

	public static void printRepoContent(UserRepository repo)
		throws Exception {
		if (user != null) {
			printNode(user, repo, "  ", null);
		} else {
			List<String> users = repo.getUsers();
			if (users != null) {
				for (String usr: users) {
					System.out.println(usr);
					printNode(usr, repo, "  ", null);
				} // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in repository.");
			} // end of else
		}
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

	public static void simpleTest(UserRepository repo) throws Exception {
		printRepoContent(repo);
		try { repo.addUser(user1); } catch (UserExistsException e) {	}
		printRepoContent(repo);
		removeTestData(repo);
		printRepoContent(repo);
	}

	public static void userAddTest(UserRepository re) throws Exception {
		UserAuthRepository repo = (UserAuthRepository)re;
		String test_user = "test111@localhost";
		printRepoContent(re);
		try { repo.addUser(test_user, "some-pass"); }
		catch (UserExistsException e) { e.printStackTrace();	}
		printRepoContent(re);
		System.out.println(
			re.getData(test_user, "privacy", "default-list", null));
		repo.removeUser(test_user);
		printRepoContent(re);
	}

	private static String help() {
		return "\n"
			+ "Parameters:\n"
      + " -h          this help message\n"
			+ " -sc class   source repository class name\n"
			+ " -su uri     source repository init string\n"
			+ " -dc class   destination repository class name\n"
			+ " -du uri     destination repository init string\n"
			+ " -dt string  data content to set/remove in repository\n"
			+ " -u user     user ID, if given all operations are only for that ID\n"
			+ "             if you want to add user to AuthRepository parameter must\n"
			+ "             in form: \"user:password\"\n"
			+ " -st         perform simple test on repository\n"
			+ " -at         simple test for adding and removing user\n"
			+ " -cp         copy content from source to destination repository\n"
			+ " -pr         print content of the repository\n"
			+ " -n          data content string is a node string\n"
			+ " -kv         data content string is node/key=value string\n"
			+ " -add        add data content to repository\n"
			+ " -del        delete data content from repository\n"
			+ "\n"
			+ "Note! If you put UserAuthRepository implementation as a class name\n"
			+ "      some operation are not allowed and will be silently skipped.\n"
			+ "      Have a look at UserAuthRepository to see what operations are\n"
			+ "      possible or what operation does make sense.\n"
			+ "      Alternatively look for admin tools guide on web site.\n"
			;
	}

	private static String src_class = "tigase.db.jdbc.JDBCRepository";
	private static String src_uri = null;
	private static String dst_class = null;
	private static String dst_uri = null;
	private static String content = null;
	private static String user = null;
	private static boolean simple_test = false;
	private static boolean add_user_test = false;
	private static boolean copy_repos = false;
	private static boolean print_repo = false;
	private static boolean add = false;
	private static boolean del = false;
	private static boolean node = false;
	private static boolean key_val = false;

	private static String subnode = null;
	private static String key = null;
	private static String value = null;

	public static void copyRepositories(UserRepository src, UserRepository dst)
		throws Exception {
		if (user != null) {
			copyUser(user, src, dst);
		} else {
			List<String> users = src.getUsers();
			if (users != null) {
				for (String usr: users) {
					copyUser(usr, src, dst);
				} // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in source repository.");
			} // end of else
		} // end of if (user != null) else
	}

	public static void copyRepositories(UserRepository src, UserAuthRepository dst)
		throws Exception {
		if (user != null) {
			copyUser(user, src, dst);
		} else {
			List<String> users = src.getUsers();
			if (users != null) {
				for (String usr: users) {
					copyUser(usr, src, dst);
				} // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in source repository.");
			} // end of else
		} // end of if (user != null) else
	}

	private static void parseNodeKeyValue(String data) {
		int val_idx = data.indexOf('=');
		value = data.substring(val_idx+1);
		String tmp_str = data.substring(0, val_idx);
		int key_idx = tmp_str.lastIndexOf('/');
		if (key_idx >= 0) {
			key = tmp_str.substring(key_idx+1);
			subnode = tmp_str.substring(0, key_idx);
		} // end of if (key_idx >= 0)
		else {
			key = tmp_str;
		} // end of if (key_idx >= 0) else
	}

	public static void parseParams(final String[] args) {
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-h")) {
          System.out.print(help());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-sc")) {
					src_class = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-su")) {
					src_uri = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-dc")) {
					dst_class = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-du")) {
					dst_uri = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-dt")) {
					content = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-st")) {
					simple_test = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-at")) {
					add_user_test = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-cp")) {
					copy_repos = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-pr")) {
					print_repo = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-u")) {
					user = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-n")) {
					node = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-kv")) {
					key_val = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-add")) {
					add = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-del")) {
					del = true;
        } // end of if (args[i].equals("-h"))
      } // end of for (int i = 0; i < args.length; i++)
    }
  }

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {

		parseParams(args);

		Exception repo_exc = null;
		UserRepository src_repo = null;
		UserAuthRepository src_auth = null;

		try {
			src_repo = RepositoryFactory.getUserRepository(src_class, src_uri);
		} catch (Exception e) {
			repo_exc = e;
			src_repo = null;
		} // end of try-catch

		// Unsuccessful UserRepository initialization
		// Let's try with AuthRepository....
		if (src_repo == null) {
			try {
				src_auth = RepositoryFactory.getAuthRepository(src_class, src_uri);
			} catch (Exception e) {
				System.out.println("Incorrect source class name given (or connection URI).");
				System.out.println("class: " + src_class);
				System.out.println("uri: " + src_uri);
				System.out.println("Can't initialize repository:");
				repo_exc.printStackTrace();
				e.printStackTrace();
				System.exit(-1);
			} // end of try-catch
		} // end of if (src_repo == null)

		if (simple_test) {
			System.out.println("Simple test on repository:");
			simpleTest(src_repo);
		} // end of if (simple_test)

		if (add_user_test) {
			System.out.println("Simple test on repository:");
			userAddTest(src_repo);
		} // end of if (simple_test)

		if (add) {
			if (key_val && src_repo != null) {
				System.out.println("Adding key=value: " + content);
				parseNodeKeyValue(content);
				System.out.println("Parsed parameters: user=" + user
					+ ", node=" + subnode + ", key=" + key + ", value=" + value);
				src_repo.setData(user, subnode, key, value);
			} else {
				System.out.println("Adding user: " + user);
				if (src_repo != null) {
					src_repo.addUser(user);
				}
				if (src_auth != null) {
					int idx = user.indexOf(':');
					String name = user;
					String password = "";
					if (idx >= 0) {
						name = user.substring(0, idx);
						password = user.substring(idx+1, user.length());
					}
					src_auth.addUser(name, password);
				}
			} // end of else
		} // end of if (add)

		if (del) {
			if (key_val || node) {
				if (key_val) {
					System.out.println("Deleting data: " + content);
					parseNodeKeyValue(content);
					System.out.println("Parsed parameters: user=" + user
						+ ", node=" + subnode + ", key=" + key + ", value=" + value);
					src_repo.removeData(user, subnode, key);
				} // end of if (key_val)
				if (node) {
					System.out.println("Deleting data node: " + content);
					src_repo.removeSubnode(user, content);
				} // end of if (node)
			} else {
				System.out.println("Deleting user: " + user);
				if (src_repo != null) {
					src_repo.removeUser(user);
				}
				if (src_auth != null) {
					src_auth.removeUser(user);
				}
			} // end of else
		} // end of if (del)

		if (copy_repos) {
			UserRepository dst_repo = null;
			Exception dst_exc = null;
			UserAuthRepository dst_auth = null;
			try {
				dst_repo = RepositoryFactory.getUserRepository(dst_class, dst_uri);
				copyRepositories(src_repo, dst_repo);
			} catch (Exception e) {
				dst_exc = e;
				dst_repo = null;
			} // end of try-catch
			if (dst_repo == null) {
				try {
					dst_auth = RepositoryFactory.getAuthRepository(dst_class, dst_uri);
				} catch (Exception e) {
					System.out.println("Incorrect destination class name given (or connection URI).");
					System.out.println("Can't initialize repository:");
					dst_exc.printStackTrace();
					e.printStackTrace();
					System.exit(-1);
				} // end of try-catch
				copyRepositories(src_repo, dst_auth);
			} // end of if (dst_repo == null)
		} // end of if (copy_repos)

		if (print_repo && src_repo != null) {
			System.out.println("Printing repository:");
			if (content != null) {
				parseNodeKeyValue(content);
			} // end of if (content != null)
			if (user != null) {
				if (key_val) {
					System.out.println(src_repo.getData(user, subnode, key, null));
				} else {
					if (node) {
						printNode(user, src_repo, "  ", subnode);
					} else {
						printNode(user, src_repo, "", null);
					} // end of else
				} // end of else
			} else {
				printRepoContent(src_repo);
			} // end of else
		} // end of if (print_repo)

	}

} // RepositoryUtils
