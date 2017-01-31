package tigase.monitor.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.disteventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.monitor.MonitorComponent;
import tigase.util.DateTimeFormatter;
import tigase.util.OSUtils;
import tigase.xml.Element;

@Bean(name = "disk-task")
public class DiskTask extends AbstractConfigurableTimerTask implements Initializable {

	public static final String DISK_USAGE_MONITOR_EVENT_NAME = "DiskUsageMonitorEvent";

	protected final static DateTimeFormatter dtf = new DateTimeFormatter();

	private static final Logger log = Logger.getLogger(DiskTask.class.getName());

	@Inject
	protected MonitorComponent component;

	@Inject
	protected EventBus eventBus;

	private File[] roots;

	protected float threshold = 0.8F;

	protected final HashSet<String> triggeredEvents = new HashSet<String>();

	public DiskTask() {
		setPeriod(1000 * 60);
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

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("threshold", "" + threshold, "Disk usage ratio threshold"));
		return x;
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
				if (line.contains("proc") || line.contains("devfs") || line.contains("tmpfs") || line.contains("sysfs")
						|| line.contains("devpts") || line.contains("securityfs")) {
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

	@Override
	public void initialize() {
		findAllRoots();
	};

	@Override
	protected void run() {
		for (File file : roots) {
			if (file.getUsableSpace() < file.getTotalSpace() * (1 - threshold)) {

				Element event = new Element(DISK_USAGE_MONITOR_EVENT_NAME, new String[] { "xmlns" },
						new String[] { MonitorComponent.EVENTS_XMLNS });
				event.addChild(new Element("hostname", component.getDefHostName().toString()));
				event.addChild(new Element("timestamp", "" + dtf.formatDateTime(new Date())));
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

}
