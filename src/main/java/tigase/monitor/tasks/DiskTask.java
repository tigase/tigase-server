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
package tigase.monitor.tasks;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.MonitorComponent;
import tigase.util.common.OSUtils;
import tigase.util.datetime.TimestampHelper;
import tigase.xml.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "disk-task", parent = MonitorComponent.class, active = true)
public class DiskTask
		extends AbstractConfigurableTimerTask
		implements Initializable {

	public static final String DISK_USAGE_MONITOR_EVENT_NAME = "tigase.monitor.tasks.DiskUsageMonitorEvent";

	protected final static TimestampHelper dtf = new TimestampHelper();

	private static final Logger log = Logger.getLogger(DiskTask.class.getName());
	protected final HashSet<String> triggeredEvents = new HashSet<String>();
	@Inject
	protected MonitorComponent component;
	@Inject
	protected EventBus eventBus;
	@ConfigField(desc = "Disk usage threshold")
	protected float threshold = 0.8F;
	private File[] roots;

	public DiskTask() {
		setPeriod(1000 * 60);
	}

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("threshold", "" + threshold, "Disk usage ratio threshold"));
		return x;
	}

	@Override
	public void initialize() {
		eventBus.registerEvent(DISK_USAGE_MONITOR_EVENT_NAME, "Fired if disk usage is too high", false);
		findAllRoots();
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field diskUsageField = form.get("threshold");
		if (diskUsageField != null) {
			this.threshold = Float.parseFloat(diskUsageField.getValue());
		}

		super.setNewConfiguration(form);
	}

	public void setThreshold(Float threshold) {
		this.threshold = threshold;
	}

	@Override
	protected void run() {
		for (File file : roots) {
			if (file.getUsableSpace() < file.getTotalSpace() * (1 - threshold)) {

				Element event = new Element(DISK_USAGE_MONITOR_EVENT_NAME);
				event.addChild(new Element("hostname", component.getDefHostName().toString()));
				event.addChild(new Element("timestamp", "" + dtf.format(new Date())));
				event.addChild(new Element("hostname", component.getDefHostName().toString()));
				event.addChild(new Element("root", file.toString()));
				event.addChild(new Element("usableSpace", "" + file.getUsableSpace()));
				event.addChild(new Element("totalSpace", "" + file.getTotalSpace()));

				if (!triggeredEvents.contains(DISK_USAGE_MONITOR_EVENT_NAME + ":" + file)) {
					eventBus.fire(event);
					triggeredEvents.add(DISK_USAGE_MONITOR_EVENT_NAME + ":" + file);
				}

			} else {
				triggeredEvents.remove(DISK_USAGE_MONITOR_EVENT_NAME + ":" + file);
			}
		}
	}

	private void findAllRoots() {
		switch (OSUtils.getOSType()) {
			case windows:
				File[] winRoots = File.listRoots();
				roots = winRoots;
				break;
			case linux:
				File[] linRoots = getLinuxRoots();
				roots = linRoots;
				break;
			case sunos:
			case solaris:
				File[] solRoots = getSolarisRoots();
				roots = solRoots;
				break;
			case mac:
				File[] macRoots = getMacRoots();
				roots = macRoots;
				break;
			default:
				File[] otherRoots = File.listRoots();
				if (otherRoots.length == 1) {
					File[] mtabRoots = getLinuxRoots();
					if (mtabRoots != null && mtabRoots.length > 1) {
						otherRoots = mtabRoots;
					}
					roots = otherRoots;
				}
		}
	}

	private File[] getLinuxRoots() {
		try {
			String mtab = "/etc/mtab";
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Reading mtab: " + mtab);
			}
			BufferedReader buffr = new BufferedReader(new FileReader(mtab));
			String line = null;
			ArrayList<File> results = new ArrayList<File>();
			while ((line = buffr.readLine()) != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Analyzing line: " + line);
				}
				if (line.contains("proc") || line.contains("devfs") || line.contains("tmpfs") ||
						line.contains("sysfs") || line.contains("devpts") || line.contains("securityfs")) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Found virtual fs line, omitting...");
					}
					continue;
				}
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Splitting line...");
				}
				String[] parts = line.split("\\s");
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Found file system: " + parts[1]);
				}
				results.add(new File(parts[1]));
			}
			return results.toArray(new File[results.size()]);
		} catch (Exception e) {
			log.warning("Can not read filesystems from /etc/mtab file" + e);
			return File.listRoots();
		}

	}

	private File[] getMacRoots() {
		File volumes = new File("/Volumes");
		return volumes.listFiles(new FileFilter() {
			@Override
			public boolean accept(File path) {
				return path.isDirectory();
			}
		});
	}

	private File[] getSolarisRoots() {
		return File.listRoots();
	}

}
