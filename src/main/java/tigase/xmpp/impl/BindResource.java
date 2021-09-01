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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.BasicComponent;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.StanzaSourceChecker;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppsession.SessionManager;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tigase.xmpp.impl.BindResource.ID;

/**
 * RFC-3920, 7. Resource Binding
 * <br>
 * Created: Mon Feb 20 21:07:29 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = ID, parent = SessionManager.class, active = true)
public class BindResource
		extends XMPPProcessor
		implements XMPPProcessorIfc, XMPPPreprocessorIfc {

	public static final String DEF_RESOURCE_PREFIX_PROP_KEY = "def-resource-prefix";
	private static final String[] COMPRESS_PATH = {"compress"};
	private static final String EL_NAME = "bind";
	private static final String[][] ELEMENTS = {Iq.IQ_BIND_PATH};
	private static final Logger log = Logger.getLogger(BindResource.class.getName());
	// protected static final String RESOURCE_KEY = "Resource-Binded";
	private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-bind";
	protected static final String ID = XMLNS;
	private static final String[] XMLNSS = {XMLNS};
	private static final Element[] FEATURES = {new Element(EL_NAME, new String[]{"xmlns"}, new String[]{XMLNS})};
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static final String RESOURCE_PREFIX_DEF = "tigase-";
	private static int resGenerator = 0;
	@Inject
	private StanzaSourceChecker stanzaSourceChecker;
	private String resourceDefPrefix = RESOURCE_PREFIX_DEF;
	@ConfigField(desc = "Automatic resource assignment prefix", alias = DEF_RESOURCE_PREFIX_PROP_KEY)
	private String resourcePrefix = null;

	public BindResource() {
		setResourcePrefix(RESOURCE_PREFIX_DEF);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		if (packet.getServerAuthorisedStanzaFrom().isPresent()) {
			// Correct user JID is already available in the packet so let's use it and short-circuit further processing.
			packet.initVars(packet.getServerAuthorisedStanzaFrom().get(), packet.getStanzaTo());
			return false;
		}
		if (session == null && stanzaSourceChecker.isPacketFromConnectionManager(packet)) {
			// rationale: packets coming from clients connections and arriving without existing session are most
			// likely send after the session has been already closed and if we don't have correct JID
			// to stamp (neither from packet itself nor from the session that doesn't exists)
			return true;
		}
		if ((session == null) || session.isServerSession() || !session.isAuthorized() ||
				C2SDeliveryErrorProcessor.isDeliveryError(packet)) {
			return false;
		}

		try {
			if (session.getConnectionId().equals(packet.getPacketFrom())) {
				// After authentication we require resource binding packet and
				// nothing else:
				// actually according to XEP-0170:
				// http://xmpp.org/extensions/xep-0170.html
				// stream compression might occur between authentication and resource
				// binding
				if (session.isResourceSet() ||
						packet.isXMLNSStaticStr(Iq.IQ_BIND_PATH, "urn:ietf:params:xml:ns:xmpp-bind") ||
						packet.isXMLNSStaticStr(COMPRESS_PATH, "http://jabber.org/protocol/compress")) {
					JID from_jid = session.getJID();

					if (from_jid != null) {

						// http://xmpp.org/rfcs/rfc6120.html#stanzas-attributes-from
						if (packet.getElemName() == tigase.server.Presence.ELEM_NAME &&
								StanzaType.getSubsTypes().contains(packet.getType()) &&
								(packet.getStanzaFrom() == null ||
										!from_jid.getBareJID().equals(packet.getStanzaFrom().getBareJID()) ||
										packet.getStanzaFrom().getResource() != null)) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Setting correct from attribute: {0}", from_jid);
							}
							packet.initVars(JID.jidInstance(from_jid.getBareJID()), packet.getStanzaTo());
						} else if ((packet.getStanzaFrom() == null) ||
								((packet.getElemName() == tigase.server.Presence.ELEM_NAME &&
										!StanzaType.getSubsTypes().contains(packet.getType()) ||
										packet.getElemName() != tigase.server.Presence.ELEM_NAME) &&
										!from_jid.equals(packet.getStanzaFrom()))) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Setting correct from attribute: {0}", from_jid);
							}
							packet.initVars(from_jid, packet.getStanzaTo());
						} else {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST,
										"Skipping setting correct from attribute: {0}, is already correct.",
										packet.getStanzaFrom());
							}
						}
					} else {
						log.log(Level.WARNING, "Session is authenticated but session.getJid() is empty: {0}",
								packet.toStringSecure());
					}
				} else {

					// We do not accept anything without resource binding....
					results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
																				  "You must bind the resource first: " +
																						  "http://www.xmpp.org/rfcs/rfc3920.html#bind",
																				  true));
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Session details: JID={0}, connectionId={1}, sessionId={2}",
								new Object[]{session.getjid(), session.getConnectionId(), session.getSessionId()});
					}

					return true;
				}
			}
		} catch (PacketErrorTypeException e) {

			// Ignore this packet
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Ignoring packet with an error to non-existen user session: {0}",
						packet.toStringSecure());
			}
		} catch (Exception e) {
			log.log(Level.FINEST, "Packet preprocessing exception: ", e);

			return false;
		}

		return false;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			results.offer(session.getAuthState().getResponseMessage(packet, "Session is not yet authorized.", false));

			return;
		}    // end of if (!session.isAuthorized())

		// TODO: test what happens if resource is bound multiple times for the same
		// user session. in particular if XMPPSession object removes the old
		// resource from the list.
		Element request = packet.getElement();
		StanzaType type = packet.getType();

		try {
			switch (type) {
				case set:
					String resource = request.getChildCDataStaticStr(Iq.IQ_BIND_RESOURCE_PATH);

					try {
						if ((resource == null) || resource.trim().isEmpty()) {
							resource = resourceDefPrefix + (++resGenerator);
							session.setResource(resource);
						} else {
							try {
								session.setResource(resource);
							} catch (TigaseStringprepException ex) {

								// User provided resource is invalid, generating different
								// server one
								log.log(Level.INFO, "Incrrect resource provided by the user: {0}, generating a " +
										"different one by the server.", resource);
								resource = resourceDefPrefix + (++resGenerator);
								session.setResource(resource);
							}
						}    // end of if (resource == null) else
					} catch (TigaseStringprepException ex) {
						log.log(Level.WARNING, "stringprep problem with the server generated resource: {0}", resource);
					}
					packet.initVars(session.getJID(), packet.getStanzaTo());

					// session.putSessionData(RESOURCE_KEY, "true");
					results.offer(packet.okResult(new Element("jid", session.getJID().toString()), 1));

					break;

				default:
					results.offer(
							Authorization.BAD_REQUEST.getResponseMessage(packet, "Bind type is incorrect", false));

					break;
			}    // end of switch (type)
		} catch (NotAuthorizedException e) {
			results.offer(session.getAuthState().getResponseMessage(packet, "Session is not yet authorized.", false));
		}    // end of try-catch
	}

	public void setResourcePrefix(String resourcePrefix) {
		this.resourcePrefix = resourcePrefix;
		this.resourceDefPrefix = Math.abs(DNSResolverFactory.getInstance().getDefaultHost().hashCode()) + "-" +
				(this.resourcePrefix != null ? this.resourcePrefix : resourceDefPrefix);
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session != null) && (!session.isResourceSet()) && session.isAuthorized()) {
			return FEATURES;
		} else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}
}    // BindResource
