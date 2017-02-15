/*
 * @(#)XMPPServer.java   2010.01.15 at 08:51:06 PST
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.server;

//~--- non-JDK imports --------------------------------------------------------
import tigase.conf.ConfigurationException;
import tigase.conf.ConfiguratorAbstract;
import tigase.util.ClassUtil;
import tigase.xml.XMLUtils;

//~--- classes ----------------------------------------------------------------
/**
 * Describe class XMPPServer here.
 *
 *
 * Created: Wed Nov 23 07:04:18 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public final class XMPPServer {

	@SuppressWarnings("PMD")
	/** property allowing setting up configurator implementation of
	 * {@link tigase.conf.ConfiguratorAbstract} used in Tigase.
	 */
	public static final String CONFIGURATOR_PROP_KEY = "tigase-configurator";

	/** default configurator implementation of
	 * {@link tigase.conf.ConfiguratorAbstract} used in Tigase, which is
	 * tigase.conf.Configurator. */
	private static final String DEF_CONFIGURATOR = "tigase.conf.Configurator";
	public static final String HARDENED_MODE_KEY = "hardened-mode";

	public static boolean isHardenedModeEnabled() {
		return System.getProperty( XMPPServer.HARDENED_MODE_KEY ) == null ? false
					 : Boolean.getBoolean( XMPPServer.HARDENED_MODE_KEY );
	}

	/** Field description */
	public static final String NAME = "Tigase";
	private static String serverName = "message-router";

	private static MessageRouterIfc router = null;
	private static ConfiguratorAbstract config = null;
	private static boolean inOSGi = false;

	//~--- constructors ---------------------------------------------------------
	/**
	 * Creates a new <code>XMPPServer</code> instance.
	 */
	private XMPPServer() {
	}

	//~--- get methods ----------------------------------------------------------
	/**
	 * Method description
	 *
	 *
	 *
	 */
	public static String getImplementationVersion() {
		String version = ComponentInfo.getImplementationVersion( XMPPServer.class );
		return ( version.isEmpty() ? "0.0.0-0" : version );
	}

	//~--- methods --------------------------------------------------------------
	/**
	 * Returns help regarding command line parameters
	 */
	public static String help() {
		return "\n" + "Parameters:\n"
					 + " -h               this help message\n"
					 + " -v               prints server version info\n"
					 + " -n server-name    sets server name\n";
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	@SuppressWarnings("PMD")
	public static void main( final String[] args ) {

		parseParams( args );

		System.out.println( ( new ComponentInfo( XMLUtils.class ) ).toString() );
		System.out.println( ( new ComponentInfo( ClassUtil.class ) ).toString() );
		System.out.println( ( new ComponentInfo( XMPPServer.class ) ).toString() );
		start( args );
	}

	public static void start( String[] args ) {
		Thread.setDefaultUncaughtExceptionHandler( new ThreadExceptionHandler() );

		if ( !isOSGi() ){
			String initial_config
						 = "tigase.level=ALL\n" + "tigase.xml.level=INFO\n"
							 + "handlers=java.util.logging.ConsoleHandler\n"
							 + "java.util.logging.ConsoleHandler.level=ALL\n"
							 + "java.util.logging.ConsoleHandler.formatter=tigase.util.LogFormatter\n";

			ConfiguratorAbstract.loadLogManagerConfig( initial_config );
		}

		try {
			String config_class_name = System.getProperty( CONFIGURATOR_PROP_KEY,
																										 DEF_CONFIGURATOR );

			config = (ConfiguratorAbstract) Class.forName( config_class_name ).newInstance();
			config.init( args );

			// config = new ConfiguratorOld(config_file, args);
			config.setName( "basic-conf" );

			String mr_class_name = config.getMessageRouterClassName();
			router = (MessageRouterIfc) Class.forName( mr_class_name ).newInstance();

			router.setName( serverName );
			router.setConfig( config );
			router.start();
		} catch ( ConfigurationException e ) {
			System.err.println( "" );
			System.err.println( "  --------------------------------------" );
			System.err.println( "  ERROR! Terminating the server process." );
			System.err.println( "  Invalid configuration data: " + e );
			System.err.println( "  Please fix the problem and start the server again." );
			System.exit( 1 );
		} catch ( Exception e ) {
			System.err.println( "" );
			System.err.println( "  --------------------------------------" );
			System.err.println( "  ERROR! Terminating the server process." );
			System.err.println( "  Problem initializing the server: " + e );
			System.err.println( "  Please fix the problem and start the server again." );
			e.printStackTrace();
			System.exit( 1 );
		}
	}

	public static void setOSGi( boolean val ) {
		inOSGi = val;
	}

	public static boolean isOSGi() {
		return inOSGi;
	}

	public static void stop() {
		( (AbstractMessageReceiver) router ).stop();
	}

	/**
	 * Allows obtaining {@link tigase.conf.ConfiguratorAbstract} implementation
	 * used by Tigase to handle all configuration of the server.
	 *
	 * @return implementation of {@link tigase.conf.ConfiguratorAbstract}
	 *         interface.
	 */
	@Deprecated
	public static ConfiguratorAbstract getConfigurator() {
		return config;
	}

	public static MessageReceiver getComponent( String name ) {
		return (MessageReceiver) config.getComponent( name );
	}

	/**
	 * Method description
	 *
	 *
	 * @param args
	 */
	@SuppressWarnings("PMD")
	public static void parseParams( final String[] args ) {
		if ( ( args != null ) && ( args.length > 0 ) ){
			for ( int i = 0 ; i < args.length ; i++ ) {
				if ( args[i].equals( "-h" ) ){
					System.out.print( help() );
					System.exit( 0 );
				}      // end of if (args[i].equals("-h"))

				if ( args[i].equals( "-v" ) ){
					System.out.print( version() );
					System.exit( 0 );
				}      // end of if (args[i].equals("-h"))

				if ( args[i].equals( "-n" ) ){
					if ( i + 1 == args.length ){
						System.out.print( help() );
						System.exit( 1 );
					} // end of if (i+1 == args.length)
					else {
						serverName = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

			}        // end of for (int i = 0; i < args.length; i++)
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public static String version() {
		return "\n" + "-- \n" + NAME + " XMPP Server, version: " + getImplementationVersion()
					 + "\n" + "Author:  Artur Hefczyc <artur.hefczyc@tigase.org>\n" + "-- \n";
	}
}    // XMPPServer

//~ Formatted in Sun Code Convention on 2010.01.15 at 08:51:06 PST


//~ Formatted by Jindent --- http://www.jindent.com
