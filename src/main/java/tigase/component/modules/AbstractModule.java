/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.logging.Logger;

import tigase.component.PacketWriter;
import tigase.component.responses.AsyncCallback;
import tigase.disteventbus.EventBus;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Abstract class for help building a module. It has implemented few default
 * methods from {@code Module}, {@code ContextAware} and
 * {@code InitializingModule}.
 *
 * @author bmalkow
 *
 */
public abstract class AbstractModule implements Module {

	@Inject(bean = "eventBus")
	protected EventBus eventBus;

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	protected PacketWriter writer;

	/**
	 * Fires event.
	 *
	 * @param event
	 *            event to fire.
	 */
	protected void fireEvent(Element event) {
		eventBus.fire(event);
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public PacketWriter getWriter() {
		return writer;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public void setWriter(PacketWriter writer) {
		this.writer = writer;
	}

	protected void write(Packet packet) {
		writer.write(packet);
	}

	protected void write(Packet packet, AsyncCallback asyncCallback) {
		writer.write(packet, asyncCallback);
	}

}
