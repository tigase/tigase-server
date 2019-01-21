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
package tigase.component.modules;

import tigase.component.PacketWriter;
import tigase.component.responses.AsyncCallback;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.xml.Element;

import java.util.logging.Logger;

/**
 * Abstract class for help building a module. It has implemented few default methods from {@code Module}, {@code
 * ContextAware} and {@code InitializingModule}.
 *
 * @author bmalkow
 */
public abstract class AbstractModule
		implements Module {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject(bean = "eventBus")
	protected EventBus eventBus;
	@Inject
	protected PacketWriter writer;

	public EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public PacketWriter getWriter() {
		return writer;
	}

	public void setWriter(PacketWriter writer) {
		this.writer = writer;
	}

	/**
	 * Fires event.
	 *
	 * @param event event to fire.
	 */
	protected void fireEvent(Element event) {
		eventBus.fire(event);
	}

	protected void write(Packet packet) {
		writer.write(packet);
	}

	protected void write(Packet packet, AsyncCallback asyncCallback) {
		writer.write(packet, asyncCallback);
	}

}
