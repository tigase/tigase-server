/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.bosh;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import tigase.xml.Element;

/**
 * Describe class BoshSessionCache here.
 *
 *
 * Created: Mon Feb 25 23:54:57 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshSessionCache {

  private static final Logger log =
    Logger.getLogger("tigase.server.bosh.BoshSessionCache");

	public static final String DEF_ID = "";
	public static final String ROSTER_ID = "bosh-roster";
	public static final String RESOURCE_BIND_ID = "bosh-resource-bind";
	public static final String MESSAGE_ID = "bosh-message";

	private static final SimpleDateFormat sdf =
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	/**
	 * Cache elements stored by the Bosh client. The cache elements are grouped
	 * by IDs. There can be any number of Elements under each ID.
	 */
	private Map<String, List<Element>> id_cache = null;
	/**
	 * Cached presence elements automaticaly stored by the Bosh component.
	 * There is only 1 presence element stored for each JID which means the
	 * cache stores the last presence element for each JID.
	 */
	private Map<String, Element> jid_presence = null;
	/**
	 * Cached time of the first message to/from some jid
	 * to speedup message caching processing
	 */
	protected Map<String, Long> jid_msg_start = null;

	/**
	 * Creates a new <code>BoshSessionCache</code> instance.
	 *
	 */
	public BoshSessionCache() {
		id_cache = new LinkedHashMap<String, List<Element>>();
		jid_presence = new LinkedHashMap<String, Element>();
		jid_msg_start = new LinkedHashMap<String, Long>();
	}

	public void set(String id, List<Element> data) {
		if (id == null) {
			id = DEF_ID;
		}
		List<Element> cached_data = new ArrayList<Element>();
		id_cache.put(id, cached_data);
		cached_data.addAll(data);
		log.finest("SET, id = " + id + ", DATA: " + data.toString());
	}

	public void add(String id, List<Element> data) {
		if (id == null) {
			id = DEF_ID;
		}
		List<Element> cached_data = id_cache.get(id);
		if (cached_data == null) {
			cached_data = new ArrayList<Element>();
			id_cache.put(id, cached_data);
		}
		cached_data.addAll(data);
		log.finest("ADD, id = " + id + ", DATA: " + data.toString());
	}

	public List<Element> remove(String id) {
		if (id == null) {
			id = DEF_ID;
		}
		List<Element> data = id_cache.remove(id);
		log.finest("REMOVED, id = " + id + ", DATA: " + data.toString());
		return data;
	}

	public List<Element> get(String id) {
		if (id == null) {
			id = DEF_ID;
		}
		List<Element> data = id_cache.get(id);
		log.finest("GET, id = " + id + ", DATA: " + data.toString());
		return data;
	}

	public List<Element> getAll() {
		List<Element> result = new ArrayList<Element>();
		for (List<Element> cache_data: id_cache.values()) {
			result.addAll(cache_data);
		}
		result.addAll(jid_presence.values());
		log.finest("GET_ALL, DATA: " + result.toString());
		return result;
	}

	public void addPresence(Element presence) {
		String from = presence.getAttribute("from");
		jid_presence.put(from, presence);
		log.finest("ADD_PRESENCE, from = " + from
			+ ", PRESENCE: " + presence.toString());
	}

	public List<Element> getAllPresences() {
		return new ArrayList<Element>(jid_presence.values());
	}

	public List<Element> getPresence(String ... from) {
		List<Element> result = new ArrayList<Element>();
		for (String f: from) {
			Element  presence = jid_presence.get(f);
			if (presence != null) {
				result.add(presence);
			}
		}
		return result;
	}

	public void addRoster(Element roster) {
		add(ROSTER_ID, Arrays.asList(roster));
		log.finest("ADD_ROSTER, ROSTER: " + roster.toString());
	}

	private long getMsgStartTime(String jid) {
		Long start_time = jid_msg_start.get(jid);
		if (start_time == null) {
			start_time = (System.currentTimeMillis()/1000);
			jid_msg_start.put(jid, start_time);
		}
		return start_time;
	}

	private Element createMessageHistory(String jid) {
		return new Element("iq", new Element[] {
				new Element("chat",
					new String[] {"xmlns", "with", "start"},
					new String[] {"urn:xmpp:tmp:archive", jid, sdf.format(new Date())})},
			new String[] {"type", "id"},
			new String[] {"result", ""+System.currentTimeMillis()});
	}

	private void addMsgBody(String jid, String direction, Element body) {
		long start_time = getMsgStartTime(jid);
		List<Element> msg_history_l = id_cache.get(MESSAGE_ID+jid);
		Element msg_history = null;
		if (msg_history == null) {
			msg_history = createMessageHistory(jid);
			add(MESSAGE_ID+jid, Arrays.asList(msg_history));
		} else {
			msg_history = msg_history_l.get(0);
		}
		long current_secs = (System.currentTimeMillis()/1000) - start_time;
		msg_history.findChild("/iq/chat").addChild(new Element(direction,
				new Element[] {body},
				new String[] {"secs"},
				new String[] {""+current_secs}));
	}

	public void addFromMessage(Element message) {
		Element body = message.findChild("/message/body");
		if (body == null) {
			return;
		}
		String jid = message.getAttribute("from");
		addMsgBody(jid, "from", body);
	}

	public void addToMessage(Element message) {
		Element body = message.findChild("/message/body");
		if (body == null) {
			return;
		}
		String jid = message.getAttribute("to");
		addMsgBody(jid, "to", body);
	}

}
