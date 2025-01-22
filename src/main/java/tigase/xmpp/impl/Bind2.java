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

import tigase.component.exceptions.ComponentException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.xmppsession.SessionManager;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tigase.xmpp.impl.SessionBind.SESSION_KEY;

@Bean(name = Bind2.XMLNS, parent = SessionManager.class, active = false)
public class Bind2 implements SaslAuth2.Inline {

	public static final String XMLNS = "urn:xmpp:bind:0";

	private static final Logger log = Logger.getLogger(Bind2.class.getName());

	@Inject
	private List<SaslAuth2.Inline> inlines;

	public List<SaslAuth2.Inline> getInlines() {
		return inlines;
	}

	public void setInlines(List<SaslAuth2.Inline> inlines) {
		this.inlines = inlines.stream().filter(it -> !(it instanceof Bind2)).collect(Collectors.toList());
	}

	@Override
	public boolean canHandle(XMPPResourceConnection connection, Element el) {
		return el.getName() == "bind" && el.getXMLNS() == XMLNS && connection.isAuthorized() &&
				connection.getResource() == null;
	}

	@Override
	public Element[] supStreamFeatures(Action action) {
		if (action != Action.sasl2) {
			return null;
		}
		Element bind = new Element("bind");
		bind.setXMLNS(XMLNS);
		
		Element inlineEl = new Element("inline");
		bind.addChild(inlineEl);
		for (SaslAuth2.Inline inline : inlines) {
			Element[] features = inline.supStreamFeatures(Action.bind2);
			if (features != null) {
				for (Element feature : features) {
					inlineEl.addChild(feature);
				}
			}
		}

		return new Element[]{bind};
	}

	public CompletableFuture<Result> process(XMPPResourceConnection session, JID _jid, Element action) {
		if (session == null) {
			return CompletableFuture.failedFuture(new ComponentException(Authorization.UNEXPECTED_REQUEST));
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			return CompletableFuture.failedFuture(new ComponentException(Authorization.NOT_AUTHORIZED));
		}    // end

		StringBuilder resourceBuilder = new StringBuilder();

		String tag = parseTag(action);
		if (tag != null) {
			resourceBuilder.append(tag);
			resourceBuilder.append("/");
		}

		SaslAuth2.UserAgent userAgent = (SaslAuth2.UserAgent) session.getSessionData(SaslAuth2.USER_AGENT_KEY);

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			if (userAgent != null && userAgent.getId() != null) {
				md.update(userAgent.getId().getBytes(StandardCharsets.UTF_8));
			} else {
				md.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
			}
			resourceBuilder.append(Base64.encode(md.digest()), 0, 12);
			JID boundJID = session.getJID().copyWithResource(resourceBuilder.toString());

			Element response = new Element("bound");
			response.setXMLNS(XMLNS);

			CompletableFuture<SaslAuth2.Inline.Result> result = CompletableFuture.completedFuture(new SaslAuth2.Inline.Result(null, true));
			List<Element> features = action.getChildren();
			if (features != null) {
				for (Element feature : features) {
					for (SaslAuth2.Inline inline : inlines) {
						if (inline.canHandle(session, feature)) {
							result = result.thenCompose(r -> {
								if (r.element != null) {
									response.addChild(r.element);
								}
								if (r.shouldContinue) {
									return inline.process(session, boundJID, feature);
								} else {
									return CompletableFuture.completedFuture(new SaslAuth2.Inline.Result(null, false));
								}
							});
						}
					}
				}
			}
			
			session.setResource(resourceBuilder.toString());
			session.putSessionData(SESSION_KEY, "true");

			return result.thenApply(r -> {
				if (r.element != null) {
					response.addChild(r.element);
				}
				return new Result(response, true);
			});

		} catch (NotAuthorizedException ex) {
			return CompletableFuture.failedFuture(new ComponentException(Authorization.NOT_AUTHORIZED));
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING, "stringprep problem with the server generated resource: {0}", resourceBuilder.toString());
			return CompletableFuture.failedFuture(new ComponentException(Authorization.INTERNAL_SERVER_ERROR));
		} catch (NoSuchAlgorithmException e) {
			return CompletableFuture.failedFuture(new ComponentException(Authorization.INTERNAL_SERVER_ERROR));
		}
	}

	private String parseTag(Element action) {
		Element tagEl = action.getChild("tag");
		return tagEl != null ? tagEl.getCData() : null;
	}
	
}
