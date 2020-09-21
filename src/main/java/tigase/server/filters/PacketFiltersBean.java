/*
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
package tigase.server.filters;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.PacketFilterIfc;
import tigase.server.QueueType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketFiltersBean implements RegistrarBean {

	private final QueueType queueType;

	private String name;
	
	@Inject
	private CopyOnWriteArrayList<PacketFilterIfc> filters = new CopyOnWriteArrayList<>();

	protected PacketFiltersBean(QueueType queueType) {
		this.queueType = queueType;
	}
	
	public void setName(String name) {
		this.name = name;
		initializeFilters();
	}

	public void setFilters(List<PacketFilterIfc> filters) {
		this.filters = new CopyOnWriteArrayList<>(filters);
		initializeFilters();
	}

	public List<PacketFilterIfc> getFilters() {
		return filters;
	}

	@Override
	public void register(Kernel kernel) {
		// nothing to do, we just need a scope
	}

	@Override
	public void unregister(Kernel kernel) {
		// nothing to do, we just need a scope
	}

	protected void initializeFilters() {
		this.filters.forEach(filter -> filter.init(Optional.ofNullable(name).orElse("UNKNOWN"), queueType));
	}

	@Bean(name = "incomingFilters", parent = AbstractMessageReceiver.class, active = true)
	public static class IncomingPacketFiltersBean extends PacketFiltersBean {

		public IncomingPacketFiltersBean() {
			super(QueueType.IN_QUEUE);
		}

	}

	@Bean(name = "outgoingFilters", parent = AbstractMessageReceiver.class, active = true)
	public static class OutgoingPacketFiltersBean extends PacketFiltersBean {

		public OutgoingPacketFiltersBean() {
			super(QueueType.OUT_QUEUE);
		}

	}
}
