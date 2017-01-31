package tigase.disteventbus.impl;

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

	public void delete(String name, String xmlns, M data) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventXmlns = xmlns == null ? NULL_NAME : xmlns;

		Map<String, Collection<M>> namesData = dataMap.get(eventXmlns);
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
			dataMap.remove(eventXmlns);
		}
	}

	public Collection<M> get(String name, String xmlns) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventXmlns = xmlns == null ? NULL_NAME : xmlns;

		Map<String, Collection<M>> namesData = dataMap.get(eventXmlns);
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
		Iterator<Entry<String, Map<String, Collection<M>>>> xmlnsIt = dataMap.entrySet().iterator();

		while (xmlnsIt.hasNext()) {
			Entry<String, Map<String, Collection<M>>> e = xmlnsIt.next();
			final String xmlns = e.getKey();

			Iterator<String> namesIt = e.getValue().keySet().iterator();
			while (namesIt.hasNext()) {
				String n = namesIt.next();
				result.add(new EventName(n == NULL_NAME ? null : n, xmlns == NULL_NAME ? null : xmlns));
			}
		}

		return result;
	}

	public boolean hasData(String name, String xmlns) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventXmlns = xmlns == null ? NULL_NAME : xmlns;

		Map<String, Collection<M>> namesData = dataMap.get(eventXmlns);
		if (namesData == null || namesData.isEmpty())
			return false;

		Collection<M> dataList = namesData.get(eventName);
		return !(dataList == null || dataList.isEmpty());
	}

	public void put(String name, String xmlns, M data) {
		final String eventName = name == null ? NULL_NAME : name;
		final String eventXmlns = xmlns == null ? NULL_NAME : xmlns;

		Map<String, Collection<M>> namesData = dataMap.get(eventXmlns);
		if (namesData == null) {
			namesData = createNamesDataMap();
			dataMap.put(eventXmlns, namesData);
		}

		Collection<M> dataList = namesData.get(eventName);
		if (dataList == null) {
			dataList = createDataList();
			namesData.put(eventName, dataList);
		}

		dataList.add(data);
	}

}
