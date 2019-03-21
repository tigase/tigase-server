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
package tigase.server.monitor;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.eventbus.TickMinuteEvent;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.XMPPServer;
import tigase.sys.TigaseRuntime;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "status-report-generator", parent = Kernel.class, active = true)
@Autostart
public class StatusReportGenerator
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(StatusReportGenerator.class.getName());
	@Inject
	private EventBus eventBus;
	@ConfigField(desc = "Status report generator enabled")
	private boolean reportGeneratorEnabled = true;

	private static StringBuilder append(StringBuilder sb, String name, String value) {
		sb.append("'").append(name).append("': ");
		sb.append("'").append(value).append("'");
		return sb;
	}

	private static StringBuilder append(StringBuilder sb, String name, int value) {
		sb.append("'").append(name).append("': ");
		sb.append(value);
		return sb;
	}

	private static StringBuilder append(StringBuilder sb, String name, double value) {
		sb.append("'").append(name).append("': ");
		sb.append(String.format(Locale.ROOT, "%.2f", value));
		return sb;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		if (reportGeneratorEnabled) {
			writeServerStatusFile();
		}
	}

	public void writeServerStatusFile() {
		try (Writer w = new FileWriter("logs/server-info.html")) {
			processTemplate(w);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot write server-info.html file", e);
		}

	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@HandleEvent
	void handleTickEvent(final TickMinuteEvent event) {
		if (reportGeneratorEnabled) {
			writeServerStatusFile();
		}
	}

	private String prepareJSON() {
		final TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();

		final StringBuilder sb = new StringBuilder();
		sb.append("{");

		append(sb, "data-uptime", runtime.getUptimeString()).append(",");
		append(sb, "data-load-average", runtime.getLoadAverage()).append(",");
		append(sb, "data-cpus-no", runtime.getCPUsNumber()).append(",");
		append(sb, "data-threads-count", runtime.getThreadsNumber()).append(",");

		append(sb, "data-cpu-usage-proc", runtime.getCPUUsage()).append(",");
		append(sb, "data-heap-usage-proc", runtime.getHeapMemUsage()).append(",");
		append(sb, "data-nonheap-usage-proc", runtime.getNonHeapMemUsage()).append(",");

		SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		append(sb, "version", XMPPServer.getImplementationVersion()).append(",");
		append(sb, "report-creation-timstamp", dtf.format(new Date()));
		sb.append("}");
		return sb.toString();
	}

	private void processTemplate(final Writer writer) throws IOException, ClassNotFoundException {
		final GStringTemplateEngine templateEngine = new GStringTemplateEngine();
		try (InputStream in = StatusReportGenerator.class.getResourceAsStream("/templates/StatusReportTemplate.html")) {
			final Template template = templateEngine.createTemplate(new InputStreamReader(in));
			Map context = new HashMap();
			context.put("dataJson", prepareJSON());

			Writable result = template.make(context);
			result.writeTo(writer);
		}

	}

}
