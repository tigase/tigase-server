/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.server;

import tigase.conf.ConfigReader;
import tigase.conf.ConfiguratorAbstract;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.events.StartupFinishedEvent;
import tigase.kernel.KernelException;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.BeanConfig;
import tigase.sys.TigaseRuntime;
import tigase.util.ClassUtil;
import tigase.util.ExceptionUtilities;
import tigase.util.Version;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.log.LogFormatter;
import tigase.xml.XMLUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Describe class XMPPServer here.
 * <br>
 * Created: Wed Nov 23 07:04:18 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public final class XMPPServer {

	@SuppressWarnings("PMD")
	/** property allowing setting up configurator implementation of
	 * {@link tigase.conf.ConfiguratorAbstract} used in Tigase.
	 */ public static final String CONFIGURATOR_PROP_KEY = "tigase-configurator";
	public static final String NAME = "Tigase";
	/**
	 * default configurator implementation of {@link tigase.conf.ConfiguratorAbstract} used in Tigase, which is
	 * tigase.conf.Configurator.
	 */
	private static final String DEF_CONFIGURATOR = "tigase.conf.Configurator";
	private final static String[] serverVersionCandidates = new String[]{"tigase.dist.XmppServerDist",
																		 XMPPServer.class.getCanonicalName()};
	private static Bootstrap bootstrap;
	private static boolean inOSGi = false;
	private static String serverName = "message-router";

	/**
	 * Allows obtaining {@link tigase.conf.ConfiguratorAbstract} implementation used by Tigase to handle all
	 * configuration of the server.
	 *
	 * @return implementation of {@link tigase.conf.ConfiguratorAbstract} interface.
	 */
//	@Deprecated
//	public static ConfiguratorAbstract getConfigurator() {
//		return config;
//	}
	public static <T> T getComponent(String name) {
		try {
			return bootstrap.getInstance(name);
		} catch (KernelException ex) {
			Logger.getLogger(XMPPServer.class.getCanonicalName())
					.log(Level.FINEST, "failed to retrieve instance of " + name, ex);
			return null;
		}
	}

	public static <T> T getComponent(Class<T> clazz) {
		try {
			return bootstrap.getInstance(clazz);
		} catch (KernelException ex) {
			Logger.getLogger(XMPPServer.class.getCanonicalName())
					.log(Level.FINEST, "failed to retrieve instance of " + clazz, ex);
			return null;
		}
	}

	public static <T> Stream<T> getComponents(Class<T> clazz) {
		return bootstrap.getKernel()
				.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> clazz.isAssignableFrom(bc.getClazz()) && bc.getState() == BeanConfig.State.initialized)
				.map(bc -> (T) bootstrap.getInstance(bc.getBeanName()));
	}

	public static String getImplementationVersion() {
		Optional<Version> version = ComponentInfo.getImplementationVersion(serverVersionCandidates);
		return (version.isPresent() ? version.get().toString() : "0.0.0-0");
	}

	public static Version getVersion() {
		return ComponentInfo.getImplementationVersion(serverVersionCandidates).orElse(Version.ZERO);
	}

	/**
	 * Returns help regarding command line parameters
	 */
	public static String help() {
		return "\n" + "Parameters:\n" + " -h               this help message\n" +
				" -v               prints server version info\n" + " -n server-name    sets server name\n";
	}

	public static boolean isOSGi() {
		return inOSGi;
	}

	public static void setOSGi(boolean val) {
		inOSGi = val;
	}

	@SuppressWarnings("PMD")
	public static void main(final String[] args) {

		parseParams(args);

		System.out.println((new ComponentInfo(XMLUtils.class)).toString());
		System.out.println((new ComponentInfo(ClassUtil.class)).toString());
		System.out.println((new ComponentInfo(XMPPServer.class)).toString());
		ComponentInfo.of("tigase.dist.XmppServerDist").ifPresent(System.out::println);
		start(args);
	}

	@SuppressWarnings("PMD")
	public static void parseParams(final String[] args) {
		if ((args != null) && (args.length > 0)) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-h")) {
					System.out.print(help());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-v")) {
					System.out.print(version());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-n")) {
					if (i + 1 == args.length) {
						System.out.print(help());
						System.exit(1);
					} // end of if (i+1 == args.length)
					else {
						serverName = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

			}        // end of for (int i = 0; i < args.length; i++)
		}
	}

	public static void start(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new ThreadExceptionHandler());

		if (!isOSGi()) {
			String initial_config =
					"tigase.level=ALL\n" + "tigase.xml.level=INFO\n" + "handlers=java.util.logging.ConsoleHandler\n" +
							"java.util.logging.ConsoleHandler.level=ALL\n" +
							"java.util.logging.ConsoleHandler.formatter=" + LogFormatter.class.getName() + "\n";

			ConfiguratorAbstract.loadLogManagerConfig(initial_config);
		}

		try {
			bootstrap = new Bootstrap();
			bootstrap.init(args);
			bootstrap.start();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");
			if (ServerBeanSelector.getConfigType(bootstrap.getKernel()) == ConfigTypeEnum.SetupMode) {
				System.out.println("== " + sdf.format(new Date()) + " Please setup server at http://localhost:8080/\n");
			} else {
				System.out.println("== " + sdf.format(new Date()) +
										   " Server finished starting up and (if there wasn't any error) is ready to use\n");
			}
			EventBusFactory.getInstance()
					.fire(new StartupFinishedEvent(DNSResolverFactory.getInstance().getDefaultHost()));
		} catch (ConfigReader.UnsupportedOperationException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Terminating the server process.",
												 e.getMessage() + " at line " + e.getLine() + " position " +
														 e.getPosition(), "Line: " + e.getLineContent(),
												 "Please fix the problem and start the server again."});
		} catch (ConfigReader.ConfigException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Terminating the server process.",
												 "Issue with configuration file: " + e,
												 "Please fix the problem and start the server again."});
		} catch (Exception e) {
			String cause = ExceptionUtilities.getExceptionRootCause(e, true);
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Terminating the server process.",
												 "Problem initializing the server: " + cause,
												 "Please fix the problem and start the server again."});
		}
	}

	public static void stop() {
		if (bootstrap != null) {
			bootstrap.stop();
		}
	}

	public static String version() {
		return "\n" + "-- \n" + NAME + " XMPP Server, version: " + getImplementationVersion() + "\n" +
				"Author:  Artur Hefczyc <artur.hefczyc@tigase.org>\n" + "-- \n";
	}

	private XMPPServer() {
	}
}    // XMPPServer

