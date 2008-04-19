/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.logging.Level;

import tigase.server.XMPPServer;
import tigase.server.Packet;
import tigase.server.AbstractMessageReceiver;
import tigase.xml.Element;

/**
 * Describe class UpdatesChecker here.
 *
 *
 * Created: Fri Apr 18 09:35:32 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UpdatesChecker extends Thread {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.util.UpdatesChecker");

	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
  private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;
	private static final String VERSION_URL =
    "http://www.tigase.org/files/downloads/tigase-server/descript.ion";
	private static final String FILE_START = "tigase-server-";

	private AbstractMessageReceiver receiver = null;
	private long interval = 7 * DAY;
	private boolean stopped = false;
	private String intro_msg = null;

	private int major_ver = 0;
	private int minor_ver = 0;
	private int bugfix_ver = 0;

	public UpdatesChecker(long interval, AbstractMessageReceiver receiver,
		String intro_msg) {
		super();
		this.interval = interval * DAY;
		//		this.interval = 30*SECOND;
		this.receiver = receiver;
		this.intro_msg = intro_msg;
		setName("UpdatesChecker");
	}

	public void run() {
		String version = XMPPServer.getImplementationVersion();
		int idx = version.indexOf('-');
		version = version.substring(0, idx);
		log.warning("Server version: " + version);
		String[] nums = version.split("\\.");
		try {
			major_ver = Integer.parseInt(nums[0]);
			minor_ver = Integer.parseInt(nums[1]);
			bugfix_ver = Integer.parseInt(nums[2]);
		} catch (NumberFormatException e) {
			log.warning("Can not detect the server version.... " + version);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem parsing server version.... " + version, e);
		}
		while (!stopped) {
			try {
				sleep(interval);
				URLConnection connection = new URL(VERSION_URL).openConnection();
				connection.setConnectTimeout(1000*60);
				connection.setReadTimeout(1000*60);
				BufferedReader buffr =
          new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = null;
				int major = 0;
				int minor = 0;
				int bugfix = 0;
				String build = "";
				while ((line = buffr.readLine()) != null) {
					if (line.startsWith(FILE_START)) {
						String file = line.substring(FILE_START.length());
						idx = file.indexOf('-');
						version = file.substring(0, idx);
						log.info("Found version: " + version);
						nums = version.split("\\.");
						try {
							int major_t = Integer.parseInt(nums[0]);
							int minor_t = Integer.parseInt(nums[1]);
							int bugfix_t = Integer.parseInt(nums[2]);
							if ((major_t > major)
								|| (major_t == major && minor_t > minor)
								|| (major_t == major && minor_t == minor && bugfix_t > bugfix)) {
								major = major_t;
								minor = minor_t;
								bugfix = bugfix_t;
								int b_idx = file.indexOf('.', idx);
								build = file.substring(idx, b_idx);
							}
						} catch (NumberFormatException e) {
							log.warning("Problem detecting new server version.... " + version);
						}
					}
				}
 				if ((major > major_ver)
					|| (major == major_ver && minor > minor_ver)
					|| (major == major_ver && minor == minor_ver && bugfix > bugfix_ver)) {
					String os_name = System.getProperty("os.name");
					String link = null;
					if (os_name.toLowerCase().contains("windows")) {
						link = "http://www.tigase.org/files/downloads/tigase-server/tigase-server-"
            + major + "." + minor + "." + bugfix + build + ".exe";
					} else {
						link = "http://www.tigase.org/files/downloads/tigase-server/tigase-server-"
            + major + "." + minor + "." + bugfix + build + ".tar.gz";
					}
					Element message = new Element("message",
            new String[] {"to", "from"},
						new String[] {receiver.getDefHostName(),
													"updates.checker@" + receiver.getDefHostName()});
					Element subject = new Element("subject",
						"Updates checker - new version of the Tigase server");
					message.addChild(subject);
					Element body = new Element("body",
						"You are currently using: '"
						+ major_ver + "." + minor_ver + "." + bugfix_ver + "' version of Tigase"
						+ " server. A new version of the server has been released: '"
						+ major + "." + minor + "." + bugfix + "' and it is available for"
						+ " download at address: " + link + "\n\n" + intro_msg);
					message.addChild(body);
					receiver.addPacket(new Packet(message));
				}
			} catch (IOException e) {
				log.log(Level.WARNING, "Can not check updates for URL: " + VERSION_URL, e);
			} catch (InterruptedException e) {
				stopped = true;
			} catch (Exception e) {
				log.log(Level.WARNING, "Unknown exception for: " + VERSION_URL, e);
			}
		}
	}

}
