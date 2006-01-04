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
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPServer {

  static String config_file = "tigase-config.xml";
	static String server_name = "tigase-xmpp-server";
  static boolean debug = false;
  static boolean monit = false;

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

  public static String version() {
    return "\n"
      + "-- \n"
      + "Tigase XMPP Server, version: "
      + XMPPServer.class.getPackage().getImplementationVersion() + "\n"
      + "Author:	Artur Hefczyc <artur.hefczyc@gmail.com>\n"
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

    parseParams(args);

		Configurator config = new Configurator(config_file);
		MessageRouter router = new MessageRouter();
		router.setName(server_name);
		router.addRegistrator(config);
		router.start();

	}


} // XMPPServer
