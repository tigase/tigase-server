package tigase.monitor.tasks;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.MonitorComponent;
import tigase.util.DateTimeFormatter;
import tigase.xml.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "cpu-temp-task", active = true)
public class CpuTempTask extends AbstractConfigurableTimerTask implements Initializable {

	public static final String CPU_TEMP_MONITOR_EVENT_NAME = "tigase.monitor.tasks.CPUTempMonitorEvent";

	private final static DateTimeFormatter dtf = new DateTimeFormatter();

	private static final File FREQ_FILE = new File("/proc/cpuinfo");

	private static final Logger log = Logger.getLogger(CpuTempTask.class.getName());

	private static final File TEMP_FILE = new File("/proc/acpi/thermal_zone/TZ01/temperature");

	private static final String THROTT_DIR = "/proc/acpi/processor/CPU";
	private static final String THROTT_FILE = "/throttling";
	private final HashSet<String> triggeredEvents = new HashSet<String>();
	@Inject
	private MonitorComponent component;
	private float[] cpu_freq = new float[Runtime.getRuntime().availableProcessors()];
	private int cpu_temp;
	private int[] cpu_thrott_pr = new int[Runtime.getRuntime().availableProcessors()];
	private int[] cpu_thrott_st = new int[Runtime.getRuntime().availableProcessors()];
	@ConfigField(desc = "CPU Temperature threshold")
	private int cpuTempThreshold = 90;
	@Inject
	private EventBus eventBus;

	public CpuTempTask() {
		setPeriod(1000 * 10);
	}

	private void checkCPUFrequency() {
		try {
			int cpu = 0;
			BufferedReader buffr = new BufferedReader(new FileReader(FREQ_FILE));
			String line = null;
			while ((line = buffr.readLine()) != null) {
				if (line.startsWith("cpu MHz")) {
					int idx = line.indexOf(':');
					cpu_freq[cpu++] = Float.parseFloat(line.substring(idx + 1).trim());
				}
			}
			buffr.close();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Can't read file: " + FREQ_FILE, ex);
		}
	}

	private void checkCPUTemperature() {
		try {
			BufferedReader buffr = new BufferedReader(new FileReader(TEMP_FILE));
			String line = buffr.readLine();
			if (line != null) {
				cpu_temp = Integer.parseInt(line.substring("temperature:".length(), line.length() - 1).trim());
			} else {
				log.warning("Empty file: " + TEMP_FILE);
			}
			buffr.close();
		} catch (FileNotFoundException ex) {
			log.log(Level.WARNING, "File contains temperature doesn't exists. Disabling task cpu-temp-task");
			setEnabled(false);
		} catch (Exception ex) {
			log.log(Level.WARNING, "Can't read file: " + TEMP_FILE, ex);
		}
	}

	private void checkCPUThrottling() {
		for (int i = 0; i < cpu_thrott_st.length; i++) {
			try {
				File file = new File(THROTT_DIR + i + THROTT_FILE);
				BufferedReader buffr = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = buffr.readLine()) != null) {
					String line_trimmed = line.trim();
					if (line_trimmed.startsWith("*")) {
						int idx = line_trimmed.indexOf(':');
						cpu_thrott_st[i] = Integer.parseInt(line_trimmed.substring(2, idx));
						String line_pr = line_trimmed.substring(idx + 1, line_trimmed.length() - 1).trim();
						cpu_thrott_pr[i] = Integer.parseInt(line_pr);
					}
				}
				buffr.close();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Can't read file: " + THROTT_DIR + i + THROTT_FILE, ex);
			}
		}
	}

	public int getCpuTempThreshold() {
		return cpuTempThreshold;
	}

	public void setCpuTempThreshold(Integer cpuTempThreshold) {
		this.cpuTempThreshold = cpuTempThreshold;
	}

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("cpuTempThreshold", "" + cpuTempThreshold, "CPU Temp threshold"));
		// x.addField(Field.fieldTextSingle("N270#cpuTemp", "" +
		// cpuTempThreshold, "CPU Temp threshold"));
		return x;
	}

	@Override
	public void initialize() {
		eventBus.registerEvent(CPU_TEMP_MONITOR_EVENT_NAME, "Fired when CPU temperature is too high", false);
	}

	@Override
	protected void run() {
		checkCPUTemperature();
		// checkCPUFrequency();
		// checkCPUThrottling();

		if (cpu_temp >= cpuTempThreshold) {
			Element event = new Element(CPU_TEMP_MONITOR_EVENT_NAME);
			event.addChild(new Element("hostname", component.getDefHostName().toString()));
			event.addChild(new Element("timestamp", "" + dtf.formatDateTime(new Date())));
			event.addChild(new Element("cpuTemp", "" + cpu_temp));

			if (!triggeredEvents.contains(event.getName())) {
				eventBus.fire(event);
				triggeredEvents.add(event.getName());
			}

		} else {
			triggeredEvents.remove(CPU_TEMP_MONITOR_EVENT_NAME);
		}
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field cpuTempField = form.get("cpuTempThreshold");
		if (cpuTempField != null) {
			this.cpuTempThreshold = Integer.parseInt(cpuTempField.getValue());
		}

		super.setNewConfiguration(form);
	}
}
