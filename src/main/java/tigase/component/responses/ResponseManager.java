package tigase.component.responses;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import tigase.component.Context;
import tigase.server.Packet;
import tigase.xmpp.JID;

public class ResponseManager {

	protected static final class Entry {

		private final AsyncCallback callback;

		private final JID jid;

		private final long timeout;

		private final long timestamp;

		public Entry(JID jid, long timestamp, long timeout, AsyncCallback callback) {
			super();
			this.jid = jid;
			this.timestamp = timestamp;
			this.timeout = timeout;
			this.callback = callback;
		}

		AsyncCallback getCallback() {
			return callback;
		}

		JID getJid() {
			return jid;
		}

		long getTimeout() {
			return timeout;
		}

		long getTimestamp() {
			return timestamp;
		}

	}

	public static final long DEFAULT_TIMEOUT = 1000 * 60;

	private final Map<String, Entry> handlers = new HashMap<String, Entry>();

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public ResponseManager(Context context) {
	}

	public void checkTimeouts() {
		long now = (new Date()).getTime();
		Iterator<java.util.Map.Entry<String, Entry>> it = this.getHandlers().entrySet().iterator();
		while (it.hasNext()) {
			java.util.Map.Entry<String, Entry> e = it.next();
			if (e.getValue().timestamp + e.getValue().timeout < now) {
				it.remove();
				e.getValue().callback.onTimeout();
			}
		}
	}

	protected Map<String, Entry> getHandlers() {
		return handlers;
	}

	/**
	 * Returns handler for response of sent {@code  <iq/>} stanza.
	 * 
	 * @param element
	 *            response {@code  <iq/>} stanza.
	 * @return Runnable object with handler
	 */
	public Runnable getResponseHandler(final Packet element) {
		if (!"iq".equals(element.getElemName()))
			return null;

		final String type = element.getElement().getAttributeStaticStr("type");
		if (type == null || type.equals("set") || type.equals("get"))
			return null;

		final String id = element.getElement().getAttributeStaticStr("id");
		if (id == null)
			return null;
		final Entry entry = this.getHandlers().get(id);
		if (entry == null)
			return null;

		if (!verify(element, entry))
			return null;

		this.getHandlers().remove(id);

		Runnable r = new DefaultResponseHandler(element, entry);
		return r;
	}

	/**
	 * Register callback for response of sent {@code <iq/>} stanza.
	 * 
	 * @param stanza
	 *            sent {@code <iq/>} stanza.
	 * @param timeout
	 *            timeout. After it method
	 *            {@linkplain AsyncCallback#onTimeout() onTimeout()} will be
	 *            called.
	 * @param callback
	 *            callback
	 * @return id of stanza
	 */
	public String registerResponseHandler(final Packet stanza, final Long timeout, final AsyncCallback callback) {
		if (stanza == null)
			return null;
		JID to = stanza.getStanzaTo();
		String id = stanza.getElement().getAttributeStaticStr("id");
		if (id == null) {
			id = UUID.randomUUID().toString();
			stanza.getElement().setAttribute("id", id);
		}

		if (callback != null) {
			Entry entry = new Entry(to, (new Date()).getTime(), timeout == null ? DEFAULT_TIMEOUT : timeout, callback);
			this.getHandlers().put(id, entry);
		}

		return id;
	}

	private boolean verify(final Packet response, final Entry entry) {
		final JID jid = response.getStanzaFrom();

		if (jid != null && entry.jid != null && jid.getBareJID().equals(entry.jid.getBareJID())) {
			return true;
		} else if (entry.jid == null && jid == null) {
			return true;
		}
		return false;
	}
}