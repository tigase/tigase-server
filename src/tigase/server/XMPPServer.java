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
package tigase.server;

import tigase.conf.Configurator;

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

	public static final String NAME = "Tigase 2";

	private static String config_file = "tigase-config.xml";
	private static String server_name = "tigase-xmpp-server";
  private static boolean debug = false;
  private static boolean monit = false;
	private static String tigaseVersion = null;

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
// 		System.out.println("package.getName()="
// 			+XMPPServer.class.getPackage().getName());
// 		System.out.println("package.getSpecificationTitle()="
// 			+XMPPServer.class.getPackage().getSpecificationTitle());
// 		System.out.println("package.getSpecificationVersion()="
// 			+XMPPServer.class.getPackage().getSpecificationVersion());
// 		System.out.println("package.getSpecificationVendor()="
// 			+XMPPServer.class.getPackage().getSpecificationVendor());
// 		System.out.println("package.getImplementationTitle()="
// 			+XMPPServer.class.getPackage().getImplementationTitle());
// 		System.out.println("package.getImplementationVersion()="
// 			+XMPPServer.class.getPackage().getImplementationVersion());
// 		System.out.println("package.getImplementationVendor()="
// 			+XMPPServer.class.getPackage().getImplementationVendor());
		return XMPPServer.class.getPackage().getImplementationVersion();
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
			".level=ALL\n"
			+ "handlers=java.util.logging.ConsoleHandler\n"
			+ "java.util.logging.ConsoleHandler.formatter=tigase.util.LogFormatter\n"
			;
		Configurator.loadLogManagerConfig(initial_config);

		Configurator config = new Configurator(config_file);
		MessageRouter router = new MessageRouter();
		router.setName(server_name);
		router.setConfig(config);
		router.start();

	}


} // XMPPServer
