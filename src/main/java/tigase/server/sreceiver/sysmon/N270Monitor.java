/*
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

package tigase.server.sreceiver.sysmon;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.stats.StatisticsList;

/**
 * Created: Jun 20, 2009 3:42:32 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class N270Monitor extends AbstractMonitor {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
					Logger.getLogger(N270Monitor.class.getName());

	private static final String TEMP_FILE =
					"/proc/acpi/thermal_zone/TZ01/temperature";
	private static final String THROTT_DIR = "/proc/acpi/processor/CPU";
	private static final String THROTT_FILE =	"/throttling";
	private static final String FREQ_FILE = "/proc/cpuinfo";
	private int cpu_temp = 0;
	private int[] cpu_thrott_st = new int[Runtime.getRuntime().availableProcessors()];
	private int[] cpu_thrott_pr = new int[Runtime.getRuntime().availableProcessors()];
	private float[] cpu_freq = new float[Runtime.getRuntime().availableProcessors()];

	@Override
	public void destroy() {
	  // Nothing to destroy
	}

	@Override
	public void check10Secs(Queue<Packet> results) {
		checkCPUTemperature();
		checkCPUFrequency();
		checkCPUThrottling();
	}

	private void checkCPUFrequency() {
		try {
			int cpu = 0;
			BufferedReader buffr = new BufferedReader(new FileReader(FREQ_FILE));
			String line = null;
			while ((line = buffr.readLine()) != null) {
				if (line.startsWith("cpu MHz")) {
					int idx = line.indexOf(':');
					cpu_freq[cpu++] = Float.parseFloat(line.substring(idx+1).trim());
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
				cpu_temp = Integer.parseInt(line.substring("temperature:".length(),
								line.length()-1).trim());
			} else {
				log.warning("Empty file: " + TEMP_FILE);
			}
			buffr.close();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Can't read file: " + TEMP_FILE, ex);
		}
	}

	private void checkCPUThrottling() {
		for (int i = 0; i < cpu_thrott_st.length; i++) {
			try {
				BufferedReader buffr = 
								new BufferedReader(new FileReader(THROTT_DIR + i + THROTT_FILE));
				String line = null;
				while ((line = buffr.readLine()) != null) {
          String line_trimmed = line.trim();
					if (line_trimmed.startsWith("*")) {
						int idx = line_trimmed.indexOf(':');
						cpu_thrott_st[i] = Integer.parseInt(line_trimmed.substring(2, idx));
						String line_pr = line_trimmed.substring(idx+1, 
										line_trimmed.length()-1).trim();
						cpu_thrott_pr[i] = Integer.parseInt(line_pr);
					}
				}
				buffr.close();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Can't read file: " +
								THROTT_DIR + i + THROTT_FILE, ex);
			}
		}
	}

	@Override
	public String getState() {
		StringBuilder sb = new StringBuilder("CPU temperature: " + cpu_temp + " C\n");
		for (int i = 0; i < cpu_thrott_st.length; i++) {
			sb.append("CPU " + i + ": FREQ: " + cpu_freq[i] +
							" MHz, Throtting: T" + cpu_thrott_st[i] +
							" - " + cpu_thrott_pr[i] + "%\n");
		}
		return sb.toString();
	}

	private final static String N270_MON = "cpu-mon";

	@Override
	public void getStatistics(StatisticsList list) {
    super.getStatistics(list);
		list.add(N270_MON, "CPU temp", this.cpu_temp, Level.INFO);
		if (list.checkLevel(Level.FINE)) {
			StringBuilder cpu_freq_str = new StringBuilder();
			StringBuilder cpu_thr_str = new StringBuilder();
			for (int i = 0; i < cpu_thrott_st.length; i++) {
				if (cpu_freq_str.length() > 0) {
					cpu_freq_str.append(", ");
				}
				cpu_freq_str.append("CPU").append(i).append(": ");
				cpu_freq_str.append(cpu_freq[i]).append(" MHz");
				if (cpu_thr_str.length() > 0) {
					cpu_thr_str.append(", ");
				}
				cpu_thr_str.append("CPU").append(i).append(": T");
				cpu_thr_str.append(cpu_thrott_st[i]).append(" - ");
				cpu_thr_str.append(cpu_thrott_pr[i]).append("%");
			}
			list.add(N270_MON, "CPU freq", cpu_freq_str.toString(), Level.FINE);
			list.add(N270_MON, "CPU throt", cpu_thr_str.toString(), Level.FINE);
		}
	}


}
