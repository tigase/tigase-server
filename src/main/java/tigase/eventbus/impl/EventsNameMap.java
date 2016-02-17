/*
 * EventsNameMap.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package tigase.eventbus.impl;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class EventsNameMap<M> {

	private final static String NULL_NAME = new String(new byte[] { 0 });

	private final Map<String, Map<String, Collection<M>>> dataMap = createMainDataMap();

	protected Collection<M> createDataList() {
		return new HashSet<M>();
	}

	protected Map<String, Map<String, Collection<M>>> createMainDataMap() {
		return new ConcurrentHashMap<String, Map<String, Collection<M>>>();
	}

	protected Map<String, Collection<M>> createNamesDataMap() {
		return new ConcurrentHashMap<String, Collection<M>>();
	}

	public void delete(M data) {
		Iterator<Entry<String, Map<String, Collection<M>>>> namesIt = dataMap.entrySet().iterator();
		while (namesIt.hasNext()) {
			Map<String, Collection<M>> datas = namesIt.next().getValue();
			Iterator<Entry<String, Collection<M>>> dataIt = datas.entrySet().iterator();
			while (dataIt.hasNext()) {
				Collection<M> d = dataIt.next().getValue();
				d.remove(data);

				if (d.isEmpty()) {
					dataIt.remove();
				}
			}

		}
	}

	public void delete(String pckg, String name, M data) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventPackage = pckg == null ? NULL_NAME : pckg;

		Map<String, Collection<M>> namesData = dataMap.get(eventPackage);
		if (namesData == null)
			return;

		Collection<M> dataCollection = namesData.get(eventName);
		if (dataCollection == null)
			return;

		dataCollection.remove(data);

		if (dataCollection.isEmpty()) {
			namesData.remove(eventName);
		}

		if (namesData.isEmpty()) {
			dataMap.remove(eventPackage);
		}
	}

	public Collection<M> get(String pckg, String name) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventPackage = pckg == null ? NULL_NAME : pckg;

		Map<String, Collection<M>> namesData = dataMap.get(eventPackage);
		if (namesData == null)
			return Collections.emptyList();

		Collection<M> dataList = namesData.get(eventName);
		if (dataList == null)
			return Collections.emptyList();

		return dataList;
	}

	public Collection<M> getAllData() {
		HashSet<M> result = new HashSet<M>();
		for (Map<String, Collection<M>> c1 : dataMap.values()) {
			for (Collection<M> c2 : c1.values()) {
				result.addAll(c2);
			}
		}
		return result;
	}

	public Set<EventName> getAllListenedEvents() {
		HashSet<EventName> result = new HashSet<EventName>();
		Iterator<Entry<String, Map<String, Collection<M>>>> packagesIt = dataMap.entrySet().iterator();

		while (packagesIt.hasNext()) {
			Entry<String, Map<String, Collection<M>>> e = packagesIt.next();
			final String eventPackage = e.getKey();

			Iterator<String> namesIt = e.getValue().keySet().iterator();
			while (namesIt.hasNext()) {
				String n = namesIt.next();
				result.add(new EventName(eventPackage == NULL_NAME ? null : eventPackage, n == NULL_NAME ? null : n));
			}
		}

		return result;
	}

	public boolean hasData(String pckg, String name) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventPackage = pckg == null ? NULL_NAME : pckg;

		Map<String, Collection<M>> namesData = dataMap.get(eventPackage);
		if (namesData == null || namesData.isEmpty())
			return false;

		Collection<M> dataList = namesData.get(eventName);
		return !(dataList == null || dataList.isEmpty());
	}

	public void put(String pckg, String name, M data) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventPackage = pckg == null ? NULL_NAME : pckg;

		Map<String, Collection<M>> namesData = dataMap.get(eventPackage);
		if (namesData == null) {
			namesData = createNamesDataMap();
			dataMap.put(eventPackage, namesData);
		}

		Collection<M> dataList = namesData.get(eventName);
		if (dataList == null) {
			dataList = createDataList();
			namesData.put(eventName, dataList);
		}

		dataList.add(data);
	}

}
