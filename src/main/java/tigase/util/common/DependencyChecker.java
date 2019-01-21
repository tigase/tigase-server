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
package tigase.util.common;

import tigase.server.XMPPServer;
import tigase.sys.TigaseRuntime;
import tigase.util.Version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyChecker {

	private static final Logger log = Logger.getLogger(DependencyChecker.class.getCanonicalName());

	public static void checkDependencies(Class clazz) {
		if (XMPPServer.isOSGi()) {
			// OSGi has its own dependency checking mechanism which will work just fine
			return;
		}

		// In other case we need to verify availability of required packages and versions
		try {
			Enumeration<URL> manifestUrls = clazz.getClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (manifestUrls.hasMoreElements()) {
				URL url = manifestUrls.nextElement();
				try (InputStream is = url.openStream()) {
					Manifest manifest = new Manifest(is);
					String dependencies = manifest.getMainAttributes().getValue("Tigase-Required-Dependencies");
					if (dependencies != null) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Found required dependencies " + dependencies + " in " + url);
						}
						for (String dependency : dependencies.split(",")) {
							String[] parts = dependency.split("=");
							String name = parts[0];
							Version version = Version.of(parts[1]);

							Package p = Package.getPackage(name);
							if (p == null) {
								TigaseRuntime.getTigaseRuntime()
										.shutdownTigase(new String[]{"Required package " + name +
																			 " is inaccessible. Make sure that all required jars are available in your classpath."});
							}
							if (p.getImplementationVersion() == null) {
								log.log(Level.FINE, "could not check " + name +
										" dependency version as package version is not set");
								continue;
							}

							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "looking for " + name + " in version " + version + " and found " +
										p.getImplementationVersion());
							}

							//check support for more than one requirement!
							// should be only >
							if (version.compareTo(Version.of(p.getImplementationVersion())) > 0) {
								TigaseRuntime.getTigaseRuntime()
										.shutdownTigase(new String[]{
												p.getImplementationTitle() + " is available in version " +
														p.getImplementationVersion() + " while " +
														clazz.getPackage().getImplementationTitle() + " " +
														clazz.getPackage().getImplementationVersion() +
														" requires version >= " + version});
							}
						}
					}
				}
			}

		} catch (IOException ex) {
			throw new RuntimeException("Failed to read " + JarFile.MANIFEST_NAME, ex);
		}
	}

}
