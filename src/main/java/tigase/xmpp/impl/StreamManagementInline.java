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
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.ReceiverTimeoutHandler;
import tigase.server.xmppclient.StreamManagementCommand;
import tigase.server.xmppclient.StreamManagementIOProcessor;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Bean(name = "urn:xmpp:sm:3", parent = SessionManager.class, active = true)
public class StreamManagementInline
		implements SaslAuth2.Inline {

	public static final String SESSION_RESUMPTION_ID_KEY = "session-resumption-id";

	@Inject(bean = "service", nullAllowed = true)
	private SessionManager sessionManager;

	public boolean canHandle(XMPPResourceConnection connection, Element el) {
		return switch (el.getName()) {
			case "enable", "resume" -> el.getXMLNS() == StreamManagementIOProcessor.XMLNS;
			default -> false;
		};
	}

	public Element[] supStreamFeatures(Action action) {
		return switch (action) {
			case sasl2 -> new Element[]{
					new Element("sm", new String[]{"xmlns"}, new String[]{StreamManagementIOProcessor.XMLNS})};
			case bind2 -> new Element[]{
					new Element("feature", new String[]{"var"}, new String[]{StreamManagementIOProcessor.XMLNS})};
		};
	}

	public CompletableFuture<Result> process(XMPPResourceConnection session, Element action) {
		return switch (action.getName()) {
			case "resume" -> resumeSession(session, action);
			case "enable" -> enable(session, action);
			default -> CompletableFuture.failedFuture(new ComponentException(Authorization.BAD_REQUEST));
		};
	}

	private CompletableFuture<Result> enable(XMPPResourceConnection session, Element action) {
		try {
			boolean resume = Boolean.parseBoolean(action.getAttributeStaticStr("resume"));
			Integer max = action.getAttributeStaticStr("max") != null
						  ? Integer.parseInt(action.getAttributeStaticStr("max"))
						  : null;
			Packet request = StreamManagementCommand.ENABLE.create(session.getSMComponentId(),
																   session.getConnectionId());
			Command.addCheckBoxField(request, "resume", resume);
			if (resume && max != null) {
				Command.addFieldValue(request, "max", String.valueOf(max));
			}
			CompletableFuture<Result> future = new CompletableFuture();
			sessionManager.addOutPacketWithTimeout(request, new ReceiverTimeoutHandler() {
				@Override
				public void timeOutExpired(Packet data) {
					future.complete(prepareFailed(Authorization.REMOTE_SERVER_TIMEOUT));
				}

				@Override
				public void responseReceived(Packet data, Packet result) {
					Element enabled = new Element("enabled");
					enabled.setXMLNS(StreamManagementIOProcessor.XMLNS);
					String resumptionId = Command.getFieldValue(result, "id");
					if (resumptionId != null) {
						enabled.setAttribute("id", resumptionId);
					}
					String location = Command.getFieldValue(result, "location");
					if (location != null) {
						enabled.addAttribute("location", location);
					}
					String maxTimeout = Command.getFieldValue(result, "max");
					if (maxTimeout != null) {
						enabled.addAttribute("max", maxTimeout);
					}

					future.complete(new Result(enabled, true));
				}
			}, 10, TimeUnit.SECONDS);

			return future;
		} catch (Exception ex) {
			return CompletableFuture.completedFuture(prepareFailed(Authorization.INTERNAL_SERVER_ERROR));
		}
	}

	private CompletableFuture<Result> resumeSession(XMPPResourceConnection session, Element action) {
		String resumptionId = action.getAttributeStaticStr("previd");
		int h = Integer.parseInt(action.getAttributeStaticStr("h"));
		Optional<XMPPResourceConnection> oldSessionOptional = session.getParentSession()
				.getActiveResources()
				.stream()
				.filter(sess -> resumptionId.equals(sess.getSessionData(SESSION_RESUMPTION_ID_KEY)))
				.findFirst();
		if (oldSessionOptional.isEmpty()) {
			return CompletableFuture.completedFuture(prepareFailed(Authorization.ITEM_NOT_FOUND));
		}

		try {
			Packet request = StreamManagementCommand.MOVE_STREAM.create(session.getSMComponentId(),
																		session.getConnectionId());
			Command.addFieldValue(request, "resumption-id", resumptionId);
			Command.addFieldValue(request, "h", String.valueOf(h));

			CompletableFuture<Result> future = new CompletableFuture<>();
			sessionManager.addOutPacketWithTimeout(request, new ReceiverTimeoutHandler() {
				@Override
				public void timeOutExpired(Packet data) {
					future.complete(prepareFailed(Authorization.RESOURCE_CONSTRAINT));
				}

				@Override
				public void responseReceived(Packet data, Packet result) {
					if (result.getType() == StanzaType.error) {
						Authorization auth = Authorization.INTERNAL_SERVER_ERROR;
						Element el = result.getElemChild("error");
						if (el != null) {
							List<Element> children = el.getChildren();
							if (children != null) {
								for (Element child : children) {
									Authorization tmp = Authorization.getByCondition(child.getName());
									if (tmp != null) {
										auth = tmp;
										break;
									}
								}
							}
						}
						future.complete(prepareFailed(auth));
					} else {
						try {
							Field resourceField = XMPPResourceConnection.class.getDeclaredField("resource");
							resourceField.setAccessible(true);
							resourceField.set(session, oldSessionOptional.get().getResource());
							Element resumed = new Element("resumed");
							resumed.setXMLNS(StreamManagementIOProcessor.XMLNS);
							String hResponse = Command.getFieldValue(result, "h");
							resumed.setAttribute("h", hResponse);
							future.complete(new Result(resumed, false));
						} catch (Throwable ex) {
							future.completeExceptionally(ex);
						}
					}
				}
			}, 10, TimeUnit.SECONDS);

			return future;
		} catch (NoConnectionIdException ex) {
			return CompletableFuture.completedFuture(prepareFailed(Authorization.INTERNAL_SERVER_ERROR));
		}
	}

	private Result prepareFailed(Authorization authorization) {
		Element failed = new Element("failed");
		failed.setXMLNS(StreamManagementIOProcessor.XMLNS);
		failed.withElement(authorization.getCondition(), Packet.ERROR_NS, (String) null);
		return new Result(failed, true);
	}

}
