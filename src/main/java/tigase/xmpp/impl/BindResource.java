/*
 * BindResource.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
 *
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RFC-3920, 7. Resource Binding
 *
 *
 * Created: Mon Feb 20 21:07:29 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BindResource
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	/** Field description */
	public static final String      DEF_RESOURCE_PREFIX_PROP_KEY = "def-resource-prefix";
	private static final String     EL_NAME                      = "bind";
	private static final String[][] ELEMENTS                     = {
		Iq.IQ_BIND_PATH
	};
	private static final Logger     log = Logger.getLogger(BindResource.class.getName());
	private static int              resGenerator                 = 0;

	// protected static final String RESOURCE_KEY = "Resource-Binded";
	private static final String    XMLNS  = "urn:ietf:params:xml:ns:xmpp-bind";
	private static final String    ID     = XMLNS;
	private static final String[]  XMLNSS = { XMLNS };
	private static final Element[] FEATURES = { new Element(EL_NAME, new String[] {
			"xmlns" }, new String[] { XMLNS }) };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMLNS }) };

	//~--- fields ---------------------------------------------------------------

	private String resourceDefPrefix = "tigase-";

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {

		int hostnameHash = Math.abs( DNSResolver.getDefaultHostname().hashCode() );
		
		// Init plugin configuration
		if (settings.get(DEF_RESOURCE_PREFIX_PROP_KEY) != null) {
			resourceDefPrefix = hostnameHash + "-" + settings.get(DEF_RESOURCE_PREFIX_PROP_KEY).toString();
		} else {
			resourceDefPrefix = hostnameHash + "-" + resourceDefPrefix;
		}
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			results.offer(session.getAuthState().getResponseMessage(packet,
					"Session is not yet authorized.", false));

			return;
		}    // end of if (!session.isAuthorized())

		// TODO: test what happens if resource is bound multiple times for the same
		// user session. in particular if XMPPSession object removes the old
		// resource from the list.
		Element    request = packet.getElement();
		StanzaType type    = packet.getType();

		try {
			switch (type) {
			case set :
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
							log.log(Level.INFO,
									"Incrrect resource provided by the user: {0}, generating a " +
									"different one by the server.", resource);
							resource = resourceDefPrefix + (++resGenerator);
							session.setResource(resource);
						}
					}    // end of if (resource == null) else
				} catch (TigaseStringprepException ex) {
					log.log(Level.WARNING,
							"stringprep problem with the server generated resource: {0}", resource);
				}
				packet.initVars(session.getJID(), packet.getStanzaTo());

				// session.putSessionData(RESOURCE_KEY, "true");
				results.offer(packet.okResult(new Element("jid", session.getJID().toString()),
						1));

				break;

			default :
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Bind type is incorrect", false));

				break;
			}    // end of switch (type)
		} catch (NotAuthorizedException e) {
			results.offer(session.getAuthState().getResponseMessage(packet,
					"Session is not yet authorized.", false));
		}    // end of try-catch
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
