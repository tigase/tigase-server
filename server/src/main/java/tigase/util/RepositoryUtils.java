/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.util;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.RepositoryFactory;
import tigase.db.AuthRepository;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.impl.roster.RosterAbstract;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.List;

//~--- classes ----------------------------------------------------------------

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
	private static long counter = 0;
	private static BareJID user1 = BareJID.bareJIDInstanceNS("user111@hostname");
	private static BareJID user2 = BareJID.bareJIDInstanceNS("user222@hostname");
	private static BareJID user3 = BareJID.bareJIDInstanceNS("user333@hostname");
	private static String src_class = "tigase.db.jdbc.JDBCRepository";
	private static String src_uri = null;
	private static String dst_class = null;
	private static String dst_uri = null;
	private static String content = null;
	private static BareJID user = null;
	private static boolean simple_test = false;
	private static boolean add_user_test = false;
	private static boolean copy_repos = false;
	private static boolean print_repo = false;
	private static boolean add = false;
	private static boolean del = false;
	private static boolean node = false;
	private static boolean key_val = false;
	private static boolean check_roster = false;
	private static boolean allowed_empty_groups = true;
	private static boolean import_data = false;
	private static boolean export_data = false;
	private static String subnode = null;
	private static String key = null;
	private static String value = null;
	private static String import_file = null;
	private static String export_file = null;

	//~--- methods --------------------------------------------------------------

//public static boolean checkJID(JID jid) {
//  String nick_check = JIDUtils.checkNickName(JIDUtils.getNodeNick(jid));
//  if (nick_check != null) {
//    System.out.println("      Invalid nickname - " + JIDUtils.getNodeNick(jid)
//      + ": " + nick_check);
//    return false;
//  }
//  String host_check = JIDUtils.checkNickName(JIDUtils.getNodeHost(jid));
//  if (host_check != null) {
//    System.out.println("      Invalid hostname - " + JIDUtils.getNodeHost(jid)
//      + ": " + host_check);
//    return false;
//  }
//  return true;
//}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param repo
	 * @param cont
	 *
	 * 
	 *
	 * @throws Exception
	 */
	public static boolean checkContact(BareJID user, UserRepository repo, String cont)
			throws Exception {

		// String[] keys = repo.getKeys(user, "roster/"+contact);
		JID contact = JID.jidInstanceNS(cont);
		String[] vals = repo.getDataList(user, "roster/" + contact, RosterAbstract.GROUPS);

		if ((vals == null) || (vals.length == 0)) {
			System.out.println("      Empty groups list");

			if ( !allowed_empty_groups) {
				return false;
			}
		} else {
			for (String val : vals) {
				if (val.equals("Upline Support") || val.equals("Support") || val.startsWith("Level ")) {
					System.out.println("      Invalid group: " + val);

					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param node
	 * @param src
	 * @param dst
	 *
	 * @throws Exception
	 */
	public static void copyNode(BareJID user, String node, UserRepository src, UserRepository dst)
			throws Exception {
		String[] keys = src.getKeys(user, node);

		if (keys != null) {
			for (String key : keys) {
				String[] vals = src.getDataList(user, node, key);

				if (vals != null) {
					dst.setDataList(user, node, key, vals);
				}    // end of if (vals != null) else
			}      // end of for (String key: keys)
		}        // end of if (keys != null)

		String[] nodes = src.getSubnodes(user, node);

		if (nodes != null) {
			for (String subnode : nodes) {
				copyNode(user, ((node != null) ? node + "/" + subnode : subnode), src, dst);
			}    // end of for (String node: nodes)
		}      // end of if (ndoes != null)
	}

	/**
	 * Method description
	 *
	 *
	 * @param src
	 * @param dst
	 *
	 * @throws Exception
	 */
	public static void copyRepositories(UserRepository src, UserRepository dst) throws Exception {
		if (user != null) {
			copyUser(user, src, dst);
		} else {
			List<BareJID> users = src.getUsers();

			if (users != null) {
				System.out.println("Found " + users.size() + " in the source repository.");

				for (BareJID usr : users) {
					System.out.println("Found " + usr + " in the source repository.");
					copyUser(usr, src, dst);
				}    // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in source repository.");
			}      // end of else
		}        // end of if (user != null) else
	}

	/**
	 * Method description
	 *
	 *
	 * @param src
	 * @param dst
	 *
	 * @throws Exception
	 */
	public static void copyRepositories(UserRepository src, AuthRepository dst) throws Exception {
		if (user != null) {
			copyUser(user, src, dst);
		} else {
			List<BareJID> users = src.getUsers();

			if (users != null) {
				for (BareJID usr : users) {
					copyUser(usr, src, dst);
				}    // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in source repository.");
			}      // end of else
		}        // end of if (user != null) else
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param src
	 * @param dst
	 *
	 * @throws Exception
	 */
	public static void copyUser(BareJID user, UserRepository src, UserRepository dst)
			throws Exception {
		if (user == null) {
			return;
		}

		System.out.print("Copying user: " + user + "...");

		try {
			dst.addUser(user);
			copyNode(user, null, src, dst);
			System.out.println("OK");
		} catch (UserExistsException e) {
			System.out.println("ERROR, user already exists.");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param src
	 * @param dst
	 *
	 * @throws Exception
	 */
	public static void copyUser(BareJID user, UserRepository src, AuthRepository dst)
			throws Exception {
		if ((user == null)) {
			return;
		}

		System.out.print("Copying user: " + user + "...");

		String password = src.getData(user, "password");

		try {
			dst.addUser(user, password);
			System.out.println("OK");
		} catch (UserExistsException e) {
			System.out.println("ERROR, user already exists.");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 * @param w
	 *
	 * @throws Exception
	 */
	public static void exportRoster(UserRepository repo, Writer w) throws Exception {
		if (user != null) {
			exportUserRoster(user, repo, w);
		} else {
			List<BareJID> users = repo.getUsers();

			if (users != null) {
				for (BareJID usr : users) {

					// System.out.println(usr);
					exportUserRoster(usr, repo, w);
				}    // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in repository.");
			}      // end of else
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param repo
	 * @param w
	 *
	 * @throws Exception
	 */
	public static void exportUserRoster(BareJID user, UserRepository repo, Writer w)
			throws Exception {
		System.out.println("  " + (++counter) + ". " + user + " roster: ");

		String[] contacts = repo.getSubnodes(user, "roster");

		if (contacts != null) {
			for (String contact : contacts) {
				System.out.println("    contact: " + contact);

				boolean valid = checkContact(user, repo, contact);

				if (valid) {
					System.out.println("      looks OK");

					String password = repo.getData(user, "password");
					String[] groups = repo.getDataList(user, "roster/" + contact, RosterAbstract.GROUPS);
					String contact_nick = repo.getData(user, "roster/" + contact, RosterAbstract.NAME);
					String subscription = repo.getData(user, "roster/" + contact,
						RosterAbstract.SUBSCRIPTION);
					StringBuilder sb = new StringBuilder(user.toString());

					sb.append(",");

					if (password != null) {
						sb.append(password);
					}

					sb.append("," + contact);
					sb.append(",");

					if (contact_nick != null) {
						sb.append(contact_nick);
					}

					sb.append(",");

					if (subscription != null) {
						sb.append(subscription);
					}

					if ((groups != null) && (groups.length > 0)) {
						for (String group : groups) {
							sb.append("," + group);
						}
					}

					sb.append("\n");
					w.write(sb.toString());
				} else {
					System.out.println("      should be REMOVED");

					String contact_node = "roster/" + contact;

					System.out.println("      removing node: " + contact_node);

					// repo.removeSubnode(user, contact_node);
					System.out.println("      DONE.");
				}
			}    // end of for (String node: nodes)
		} else {
			System.out.println("    empty roster...");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void loadTestData(UserRepository repo) throws Exception {
		try {
			repo.addUser(user1);
		} catch (UserExistsException e) {}

		try {
			repo.addUser(user2);
		} catch (UserExistsException e) {}

		try {
			repo.addUser(user3);
		} catch (UserExistsException e) {}

		repo.setData(user1, null, "password", "secret111");
		repo.setData(user2, null, "password", "secret222");
		repo.setData(user3, null, "password", "secret333");
		repo.setData(user1, "roster/buddy111", "name", "budy1");
		repo.setData(user1, "roster/buddy222", "name", "budy2");
		repo.setData(user1, "roster/buddy333", "name", "budy3");
		repo.setDataList(user1, "roster/buddy111", "groups", new String[] { "buddies", "friends" });
		repo.setDataList(user2, "roster/buddy111", "groups", new String[] { "buddies", "friends" });
		repo.addDataList(user2, "roster/buddy111", "groups", new String[] { "family", "home" });
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 *
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		parseParams(args);

		Exception repo_exc = null;
		UserRepository src_repo = null;
		AuthRepository src_auth = null;

		try {
			src_repo = RepositoryFactory.getUserRepository(src_class, src_uri, null);
			System.out.println("Loaded src_repo " + src_repo.getClass().getName() + " for parameters:"
					+ "\n   src_class=" + src_class + "\n   src_uri=" + src_uri);
		} catch (Exception e) {
			repo_exc = e;
			src_repo = null;
		}    // end of try-catch

		// Unsuccessful UserRepository initialization
		// Let's try with AuthRepository....
		if (src_repo == null) {
			try {
				src_auth = RepositoryFactory.getAuthRepository(src_class, src_uri, null);
				System.out.println("Loaded src_auth " + src_auth.getClass().getName() + " for parameters:"
						+ "\n   src_class=" + src_class + "\n   src_uri=" + src_uri);
			} catch (Exception e) {
				System.out.println("Incorrect source class name given (or connection URI).");
				System.out.println("class: " + src_class);
				System.out.println("uri: " + src_uri);
				System.out.println("Can't initialize repository:");
				repo_exc.printStackTrace();
				e.printStackTrace();
				System.exit(-1);
			}    // end of try-catch
		}      // end of if (src_repo == null)

		if (simple_test) {
			System.out.println("Simple test on repository:");
			simpleTest(src_repo);
		}    // end of if (simple_test)

		if (add_user_test) {
			System.out.println("Simple test on repository:");
			userAddTest(src_repo);
		}    // end of if (simple_test)

		if (add) {
			if (key_val && (src_repo != null)) {
				System.out.println("Adding key=value: " + content);
				parseNodeKeyValue(content);
				System.out.println("Parsed parameters: user=" + user + ", node=" + subnode + ", key=" + key
						+ ", value=" + value);
				src_repo.setData(user, subnode, key, value);
			} else {
				System.out.println("Adding user: " + user);

				if (src_repo != null) {
					src_repo.addUser(user);
				}

				if (src_auth != null) {
					BareJID name = user;
					String password = "";

					src_auth.addUser(name, password);
				}
			}    // end of else
		}      // end of if (add)

		if (del) {
			if (key_val || node) {
				if (key_val) {
					System.out.println("Deleting data: " + content);
					parseNodeKeyValue(content);
					System.out.println("Parsed parameters: user=" + user + ", node=" + subnode + ", key="
							+ key + ", value=" + value);
					src_repo.removeData(user, subnode, key);
				}    // end of if (key_val)

				if (node) {
					System.out.println("Deleting data node: " + content);
					src_repo.removeSubnode(user, content);
				}    // end of if (node)
			} else {
				System.out.println("Deleting user: " + user);

				if (src_repo != null) {
					src_repo.removeUser(user);
				}

				if (src_auth != null) {
					src_auth.removeUser(user);
				}
			}      // end of else
		}        // end of if (del)

		if (copy_repos) {
			UserRepository dst_repo = null;
			Exception dst_exc = null;
			AuthRepository dst_auth = null;

			try {
				dst_repo = RepositoryFactory.getUserRepository(dst_class, dst_uri, null);
				System.out.println("Loaded dst_repo " + dst_repo.getClass().getName() + " for parameters:"
						+ "\n   src_class=" + dst_class + "\n   src_uri=" + dst_uri);
				copyRepositories(src_repo, dst_repo);
			} catch (Exception e) {
				dst_exc = e;
				dst_repo = null;
			}    // end of try-catch

			if (dst_repo == null) {
				try {
					dst_auth = RepositoryFactory.getAuthRepository(dst_class, dst_uri, null);
					System.out.println("Loaded dst_auth " + dst_auth.getClass().getName()
							+ " for parameters:" + "\n   src_class=" + dst_class + "\n   src_uri=" + dst_uri);
				} catch (Exception e) {
					System.out.println("Incorrect destination class name given (or connection URI).");
					System.out.println("Can't initialize repository:");
					dst_exc.printStackTrace();
					e.printStackTrace();
					System.exit(-1);
				}    // end of try-catch

				copyRepositories(src_repo, dst_auth);
			}      // end of if (dst_repo == null)
		}        // end of if (copy_repos)

		if (check_roster && (src_repo != null)) {
			System.out.println("Checking roster:");

			if (user != null) {
				repairUserRoster(user, src_repo);
			} else {
				repairRoster(src_repo);
			}    // end of else
		}

		if (import_data && (src_repo != null)) {
			BufferedReader br = new BufferedReader(new FileReader(import_file));
			String line = null;

			while ((line = br.readLine()) != null) {
				String[] vals = line.split(",");
				BareJID userId = BareJID.bareJIDInstance(vals[0].trim());

				try {
					src_repo.addUser(userId);
				} catch (UserExistsException e) {}

				if ((vals.length >= 2) && (vals[1].trim().length() > 0)) {
					src_repo.setData(userId, null, "password", vals[1].trim());
				}

				if ((vals.length >= 3) && (vals[2].trim().length() > 0)) {
					src_repo.setData(userId, "roster/" + vals[2].trim(), "name", vals[2].trim());
				}

				if ((vals.length >= 4) && (vals[3].trim().length() > 0)) {
					src_repo.setData(userId, "roster/" + vals[2].trim(), "name", vals[3].trim());
				}

				if ((vals.length >= 5) && (vals[4].trim().length() > 0)) {
					src_repo.setData(userId, "roster/" + vals[2].trim(), "subscription", vals[4].trim());
				}

				if ((vals.length >= 6) && (vals[5].trim().length() > 0)) {
					src_repo.setData(userId, "roster/" + vals[2].trim(), "groups", vals[5].trim());
				}
			}

			br.close();
		}

		if (export_data && (src_repo != null)) {
			FileWriter fr = new FileWriter(export_file);

			if (user != null) {
				exportUserRoster(user, src_repo, fr);
			} else {
				exportRoster(src_repo, fr);
			}    // end of else

			fr.close();
		}

		if (print_repo && (src_repo != null)) {
			System.out.println("Printing repository:");

			if (content != null) {
				if (node) {
					subnode = content;
				} else {
					parseNodeKeyValue(content);
				}
			}        // end of if (content != null)

			if (user != null) {
				if (key_val) {
					System.out.println(src_repo.getData(user, subnode, key, null));
				} else {
					if (node) {
						printNode(user, src_repo, "  ", subnode);
					} else {
						printNode(user, src_repo, "", null);
					}    // end of else
				}      // end of else
			} else {
				printRepoContent(src_repo);
			}        // end of else
		}          // end of if (print_repo)
	}

	/**
	 * Method description
	 *
	 *
	 * @param args
	 *
	 * @throws TigaseStringprepException
	 */
	public static void parseParams(final String[] args) throws TigaseStringprepException {
		if ((args != null) && (args.length > 0)) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-h")) {
					System.out.print(help());
					System.exit(0);
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-sc")) {
					src_class = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-su")) {
					src_uri = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-dc")) {
					dst_class = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-du")) {
					dst_uri = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-dt")) {
					content = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-st")) {
					simple_test = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-at")) {
					add_user_test = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-cp")) {
					copy_repos = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-pr")) {
					print_repo = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-u")) {
					user = BareJID.bareJIDInstance(args[++i]);
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-n")) {
					node = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-kv")) {
					key_val = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-add")) {
					add = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-del")) {
					del = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-roster")) {
					check_roster = true;
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-import")) {
					import_data = true;
					import_file = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-export")) {
					export_data = true;
					export_file = args[++i];
				}    // end of if (args[i].equals("-h"))

				if (args[i].equals("-aeg")) {
					allowed_empty_groups = args[++i].equals("true");
				}    // end of if (args[i].equals("-h"))
			}      // end of for (int i = 0; i < args.length; i++)
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param repo
	 * @param prefix
	 * @param node
	 *
	 * @throws Exception
	 */
	public static void printNode(BareJID user, UserRepository repo, String prefix, String node)
			throws Exception {
		if (node != null) {
			System.out.println(prefix + "node: " + node);
		}    // end of if (node != null)

		String[] keys = repo.getKeys(user, node);

		if (keys != null) {
			for (String key : keys) {
				String[] vals = repo.getDataList(user, node, key);

				if (vals != null) {
					StringBuilder valstr = new StringBuilder();

					for (String val : vals) {
						valstr.append(" ").append(val);
					}    // end of for (String val: vals)

					System.out.println(prefix + "  " + key + " = " + valstr);
				} else {
					System.out.println("    " + key);
				}      // end of if (vals != null) else
			}        // end of for (String key: keys)
		}          // end of if (keys != null)

		String[] nodes = repo.getSubnodes(user, node);

		if (nodes != null) {
			for (String subnode : nodes) {
				printNode(user, repo, prefix + "  ", ((node != null) ? node + "/" + subnode : subnode));
			}    // end of for (String node: nodes)
		}      // end of if (ndoes != null)
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void printRepoContent(UserRepository repo) throws Exception {
		if (user != null) {
			printNode(user, repo, "  ", subnode);
		} else {
			List<BareJID> users = repo.getUsers();

			if (users != null) {
				for (BareJID usr : users) {
					System.out.println(usr);
					printNode(usr, repo, "  ", subnode);
				}    // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in repository.");
			}      // end of else
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void removeTestData(UserRepository repo) throws Exception {
		repo.removeUser(user1);
		repo.removeUser(user2);
		repo.removeUser(user3);
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void repairRoster(UserRepository repo) throws Exception {
		if (user != null) {
			repairUserRoster(user, repo);
		} else {
			List<BareJID> users = repo.getUsers();

			if (users != null) {
				for (BareJID usr : users) {

					// System.out.println(usr);
					repairUserRoster(usr, repo);
				}    // end of for (String user: users)
			} else {
				System.out.println("There are no user accounts in repository.");
			}      // end of else
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void repairUserRoster(BareJID user, UserRepository repo) throws Exception {
		System.out.println("  " + (++counter) + ". " + user + " roster: ");

		String[] contacts = repo.getSubnodes(user, "roster");

		if (contacts != null) {
			for (String contact : contacts) {
				System.out.println("    contact: " + contact);

				boolean valid = checkContact(user, repo, contact);

				if (valid) {
					System.out.println("      looks OK");
				} else {
					System.out.println("      should be REMOVED");

					String contact_node = "roster/" + contact;

					System.out.println("      removing node: " + contact_node);
					repo.removeSubnode(user, contact_node);
					System.out.println("      DONE.");
				}
			}    // end of for (String node: nodes)
		} else {
			System.out.println("    empty roster...");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 *
	 * @throws Exception
	 */
	public static void simpleTest(UserRepository repo) throws Exception {
		printRepoContent(repo);

		try {
			repo.addUser(user1);
		} catch (UserExistsException e) {}

		printRepoContent(repo);
		removeTestData(repo);
		printRepoContent(repo);
	}

	/**
	 * Method description
	 *
	 *
	 * @param re
	 *
	 * @throws Exception
	 */
	public static void userAddTest(UserRepository re) throws Exception {
		AuthRepository repo = (AuthRepository) re;
		BareJID test_user = BareJID.bareJIDInstanceNS("test111@localhost");

		printRepoContent(re);

		try {
			repo.addUser(test_user, "some-pass");
		} catch (UserExistsException e) {
			e.printStackTrace();
		}

		printRepoContent(re);
		System.out.println(re.getData(test_user, "privacy", "default-list", null));
		repo.removeUser(test_user);
		printRepoContent(re);
	}

	private static String help() {
		return "\n" + "Parameters:\n" + " -h          this help message\n"
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
				+ " -del        delete data content from repository\n" + " ------------\n"
				+ " -roster     check the user roster\n"
				+ " -aeg [true|false]  Allow empty group list for the contact\n"
				+ " -import file  import user data from the file of following format:\n"
				+ "         user_jid, password, roser_jid, roster_nick, subscription, group\n"
				+ " -export file  export user roster data to the specified file in the following\n"
				+ "              format: user_jid, password, roser_jid, roster_nick, subscription,\n"
				+ "               group\n" + "\n" + "\n"
				+ "Note! If you put UserAuthRepository implementation as a class name\n"
				+ "      some operation are not allowed and will be silently skipped.\n"
				+ "      Have a look at UserAuthRepository to see what operations are\n"
				+ "      possible or what operation does make sense.\n"
				+ "      Alternatively look for admin tools guide on web site.\n"
		;
	}

	private static void parseNodeKeyValue(String data) {
		int val_idx = data.indexOf('=');

		value = data.substring(val_idx + 1);

		String tmp_str = data.substring(0, val_idx);
		int key_idx = tmp_str.lastIndexOf('/');

		if (key_idx >= 0) {
			key = tmp_str.substring(key_idx + 1);
			subnode = tmp_str.substring(0, key_idx);
		}    // end of if (key_idx >= 0)
				else {
			key = tmp_str;
		}    // end of if (key_idx >= 0) else
	}
}    // RepositoryUtils


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
