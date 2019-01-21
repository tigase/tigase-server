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
import tigase.monitor.MonitorComponent;
import tigase.xml.Element;

import java.util.Date;

@Bean(name = "sample-task", parent = MonitorComponent.class, active = true)
public class SampleTask
		extends AbstractConfigurableTimerTask
		implements Initializable {

	@Inject
	private EventBus eventBus;
	private String message = "<->";

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("message", "", "Event message"));
		return x;
	}

	@Override
	public void initialize() {
		eventBus.registerEvent("tigase.monitor.tasks.SampleTaskEnabled", "Sample task", false);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field m = form.get("message");
		if (m == null) {
			this.message = "<not found>";
		} else {
			this.message = m.getValue();
		}

		super.setNewConfiguration(form);
	}

	@Override
	protected void enable() {
		super.enable();

		Element event = new Element("tigase.monitor.tasks.SampleTaskEnabled");
		event.addChild(new Element("timestamp", "" + (new Date())));
		event.addChild(new Element("message", this.message));
		this.message = "<->";
		eventBus.fire(event);

		setEnabled(false);
	}

	@Override
	protected void run() {
	}
}
