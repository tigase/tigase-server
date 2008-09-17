/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.server;

import java.util.Locale;
import tigase.conf.Configurator;
import tigase.xml.db.XMLDBException;

/**
 * Describe class XMPPServer here.
 *
 *
 * Created: Wed Nov 23 07:04:18 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPServer {

	public static final String NAME = "Tigase";

	private static String config_file = "tigase-config.xml";
	private static String server_name = "message-router";
  private static boolean debug = false;
  private static boolean monit = false;
	private static String tigaseVersion = null;
	private static boolean gen_config = false;

	/**
	 * Creates a new <code>XMPPServer</code> instance.
	 *
	 */
	protected XMPPServer() {}

  public static String help() {
    return "\n"
      + "Parameters:\n"
      + " -h                this help message\n"
      + " -v                prints server version info\n"
      + " -c file           location of configuration file\n"
      + " -d [true|false]   turn on|off debug mode\n"
      + " -m                turn on server monitor\n"
			+ " -n server-name    sets server name\n"
      ;
  }

	public static String getImplementationVersion() {
		String version = XMPPServer.class.getPackage().getImplementationVersion();
		return version == null ? "0.0.0-0" : version;
	}

	public static String version() {
    return "\n"
      + "-- \n"
      + NAME + " XMPP Server, version: "
      + getImplementationVersion() + "\n"
      + "Author:	Artur Hefczyc <artur.hefczyc@tigase.org>\n"
      + "-- \n"
      ;
  }

  public static void parseParams(final String[] args) {
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-h")) {
          System.out.print(help());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-v")) {
          System.out.print(version());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-c")) {
          if (i+1 == args.length) {
            System.out.print(help());
            System.exit(1);
          } // end of if (i+1 == args.length)
          else {
            config_file = args[++i];
          } // end of else
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-n")) {
          if (i+1 == args.length) {
            System.out.print(help());
            System.exit(1);
          } // end of if (i+1 == args.length)
          else {
            server_name = args[++i];
          } // end of else
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-d")) {
          if (i+1 == args.length) {
            debug = true;
          } // end of if (i+1 == args.length)
          else {
            ++i;
            debug = args[i].charAt(0) != '-' &&
              (args[i].equals("true") || args[i].equals("yes"));
          } // end of else
        } // end of if (args[i].equals("-d"))
        if (args[i].equals("-m")) {
          monit = true;
        } // end of if (args[i].equals("-m"))
				if (args[i].startsWith("--gen-config")) {
					gen_config = true;
				}
      } // end of for (int i = 0; i < args.length; i++)
    }
  }

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) {

		//		getImplementationVersion();

		Thread.setDefaultUncaughtExceptionHandler(new ThreadExceptionHandler());

    parseParams(args);

		String initial_config =
			"tigase.level=ALL\n"
			+ "tigase.xml.level=INFO\n"
			+ "handlers=java.util.logging.ConsoleHandler\n"
			+ "java.util.logging.ConsoleHandler.level=ALL\n"
			+ "java.util.logging.ConsoleHandler.formatter=tigase.util.LogFormatter\n"
			;
		Configurator.loadLogManagerConfig(initial_config);

		Configurator config = null;
		try {
			config = new Configurator(config_file, args);
		} catch (XMLDBException e) {
			System.err.println("");
			System.err.println("  --------------------------------------");
			System.err.println("  ERROR! Terminating the server process.");
			System.err.println("  Invalid XML configuration file: " + config_file);
			System.err.println("  Please fix the problem and start the server again.");
			System.exit(1);
		}
		config.setName("basic-conf");
		MessageRouter router = new MessageRouter();
		router.setName(server_name);
		router.setConfig(config);

		router.start();

	}


} // XMPPServer
