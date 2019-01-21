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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.LastActivityAbstract.XMLNS;
import static tigase.xmpp.impl.LastActivityMarker.ID;

/**
 * Implementation of <a href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 *
 * @author bmalkow
 */

@Id(ID)
@DiscoFeatures({XMLNS})
@Handles({@Handle(path = {Presence.ELEM_NAME}, xmlns = Packet.CLIENT_XMLNS),
		  @Handle(path = {Message.ELEM_NAME}, xmlns = Packet.CLIENT_XMLNS)})
@Bean(name = LastActivityMarker.ID, parent = SessionManager.class, active = false)
public class LastActivityMarker
		extends LastActivityAbstract
		implements XMPPStopListenerIfc, RegistrarBean {

	protected final static String ID = XMLNS + "-marker";
	private static final Logger log = Logger.getLogger(LastActivityMarker.class.getName());
	private Kernel kernel;
	@ConfigField(desc = "To persist all updates to repository")
	private boolean persistAllToRepository = true;
	@Inject
	private LastActivityRetriever[] retrievers;
	@ConfigField(desc = "Whether to update last activity information on message packets", alias = "message")
	private boolean updateOnMessage = false;
	@ConfigField(desc = "Whether to update last activity information on presence packets", alias = "presence")
	private boolean updateOnPresence = true;

	private static void setLastActivity(XMPPResourceConnection session, Long last, Element presence, boolean repository) {
		session.putCommonSessionData(LastActivityAbstract.LAST_ACTIVITY_KEY, last);
		session.putSessionData(LastActivityAbstract.LAST_ACTIVITY_KEY, last);
		if (repository) {
			persistLastActivity(session, presence);
		}
	}

	public void setRetrievers(LastActivityRetriever[] retrievers) {
		this.retrievers = retrievers;
		if (kernel != null) {
			for (LastActivityRetriever retriever : retrievers) {
				final Bean bean = retriever.getClass().getAnnotation(Bean.class);
				if (bean != null) {
					kernel.ln(bean.name(), kernel.getParent(), bean.name());
				}
			}
		}
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}

		if (session != null && packet.getStanzaFrom() != null &&
				session.isUserId(packet.getStanzaFrom().getBareJID())) {
			final long time = System.currentTimeMillis();

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Updating last:activity of user " + session.getUserName() + " to " + time);
			}

			if ((updateOnMessage && packet.getElemName() == Message.ELEM_NAME) ||
					(updateOnPresence && packet.getElemName() == Presence.ELEM_NAME)) {
				setLastActivity(session, time, packet.getElement(), persistAllToRepository);
			}
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Session stoppped for " + session.getjid() + ", persisting last activity");
		}
		if (session != null && session.isAuthorized()) {
			final Element presence = session.getPresence();
			persistLastActivity(session, presence);
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {

	}
}
