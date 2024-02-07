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
package tigase.db.util.importexport;

import tigase.db.AbstractAuthRepositoryWithCredentials;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostJDBCRepository;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xmpp.impl.VCardTemp;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Importer {
	private static final Logger log = Logger.getLogger(Importer.class.getSimpleName());
	private static final SimpleParser parser = new SimpleParser();
	enum State {
		begining,
		serverData,
		host,
		user
	}

	private final RepositoryHolder repositoryHolder;
	private final VHostJDBCRepository vhostRepository;
	private final Path rootPath;
	private State state = State.begining;
	private String domain;
	private BareJID user;
	private Stack<Element> readElements = new Stack<>();
	private Stack<String> elementStack = new Stack<>();
	private final List<RepositoryManagerExtension> extensions;
	private ImporterExtension activeExtension = null;
	private int usersCount = 0;

	public Importer(RepositoryHolder repositoryHolder, VHostJDBCRepository vhostRepository, List<RepositoryManagerExtension> extensions, Path rootPath) {
		this.repositoryHolder = repositoryHolder;
		this.vhostRepository = vhostRepository;
		this.rootPath = rootPath;
		this.extensions = extensions;
	}

	protected void startElement(String name, Map<String, String> attrs) {
		elementStack.push(name);
		if (name.equals("xi:include")) {
			String path = attrs.get("href");
			try {
				process(rootPath.resolve(path));
			} catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
		} else {
			boolean handled = switch (state) {
				case begining -> switch (name) {
					case "server-data" -> {
						state = State.serverData;
						yield true;
					}
					default -> false;
				};
				case serverData -> switch (name) {
					case "host" -> {
						state = State.host;
						domain = attrs.get("jid");
						usersCount = 0;
						log.info("importing domain " + domain + " data...");
						if (vhostRepository.getItem(domain) == null) {
							VHostItem item = vhostRepository.getItemInstance();
							item.setKey(domain);
							vhostRepository.addItem(item);
						}
						yield true;
					}
					default -> false;
				};
				case host -> switch (name) {
					case "user" -> {
						state = State.user;
						user = BareJID.bareJIDInstanceNS(attrs.get("name"), domain);
						log.info("importing user " + user + " data...");
						try {
							UserRepository userRepository = repositoryHolder.getRepository(
									UserRepository.class, user.getDomain());
							if (!userRepository.userExists(user)) {
								userRepository.addUser(user);
							}
							AuthRepository.AccountStatus accountStatus = Optional.ofNullable(attrs.get("tigase:status"))
									.map(AuthRepository.AccountStatus::valueOf)
									.orElse(AuthRepository.AccountStatus.active);
							AuthRepository authRepository = repositoryHolder.getRepository(AbstractAuthRepositoryWithCredentials.class, user.getDomain());
							authRepository.setAccountStatus(user, accountStatus);
						} catch (Throwable ex) {
							throw new RuntimeException(ex);
						}
						yield true;
					}
					default -> false;
				};
				default -> false;
			};
			if (!handled) {
				try {
					if (activeExtension == null) {
						for (RepositoryManagerExtension extension : extensions) {
							activeExtension = extensionStartImport(extension, name, attrs);
							if (activeExtension != null) {
								handled = true;
								break;
							}
						}
					} else {
						handled = activeExtension.startElement(name, attrs);
					}
				} catch (Throwable ex) {
					throw new RuntimeException(ex);
				}
			}
			if (!handled) {
				Element el = new Element(name);
				el.setAttributes(attrs);
				readElements.push(el);
			}
		}
	}

	private ImporterExtension extensionStartImport(RepositoryManagerExtension extension, String name,
												   Map<String, String> attrs) throws Exception {
		return switch (state) {
			case host -> extension.startImportDomainData(domain, name, attrs);
			case user -> extension.startImportUserData(user, name, attrs);
			default -> null;
		};
	}

	protected void elementCData(String cdata) {
		if (!readElements.isEmpty()) {
			readElements.peek().addCData(cdata);
		}
	}

	interface ThrowingFunction<IN, OUT> {

		OUT apply(IN in) throws Exception;
	}

	private boolean processReadElement(ThrowingFunction<Element, Boolean> func) throws Exception {
		if (readElements.size() == 1) {
			Element el = readElements.pop();
			return func.apply(el);
		} else {
			return false;
		}
	}

	protected boolean endElement(String name) {
		try {
			if (name.equals(elementStack.peek())) {
				if (name.equals("xi:include")) {
					elementStack.pop();
					return true;
				}
				if (activeExtension != null) {
					boolean handled = activeExtension.endElement(name);
					if (!handled) {
						if (!readElements.isEmpty()) {
							Element el = readElements.pop();
							if (readElements.isEmpty()) {
								if (!activeExtension.handleElement(el)) {
									log.warning("read unknown element: " + el);
								}
							} else {
								readElements.peek().addChild(el);
							}
						} else {
							activeExtension.close();
							activeExtension = null;
						}
					}
				} else {
					boolean handled = switch (state) {
						case serverData -> switch (name) {
							case "server-data" -> {
								state = State.begining;
								yield true;
							}
							default -> false;
						};
						case host -> switch (name) {
							case "host" -> {
								state = State.serverData;
								log.info("imported domain " + domain + " with " + usersCount + " users");
								usersCount = 0;
								domain = null;
								yield true;
							}
							default -> false;
						};
						case user -> switch (name) {
							case "user" -> {
								state = State.host;
								log.fine("imported user " + user);
								usersCount++;
								user = null;
								yield true;
							}
							case "query" -> processReadElement(query -> switch (query.getXMLNS()) {
								case "jabber:iq:roster" -> {
									saveRoster(query);
									yield true;
								}
								default -> false;
							});
							case "vCard" -> processReadElement(vcard -> switch (vcard.getXMLNS()) {
								case "vcard-temp" -> {
									UserRepository userRepository = repositoryHolder.getRepository(
											UserRepository.class, user.getDomain());
									userRepository.setData(user, "public/vcard-temp", VCardTemp.VCARD_KEY,
														   vcard.toString());
									yield true;
								}
								default -> false;
							});
							default -> false;
						};
						default -> false;
					};
					if (!handled) {
						if (!readElements.isEmpty()) {
							Element el = readElements.pop();
							if (!readElements.isEmpty()) {
								Element parentEl = readElements.peek();
								if (parentEl != null) {
									parentEl.addChild(el);
								}
							} else {
								log.warning("read unknown element: " + el);
							}
						}
					}
				}
				elementStack.pop();
				return true;
			} else {
				log.severe(
						"found close tag for " + name + " without start element at: " + elementStack.stream().toList());
				return false;
			}
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	private void saveRoster(Element query) throws Exception {
		List<Element> children = query.findChildren(el -> "item".equals(el.getName()));
		List<RosterElement> roster = new ArrayList<>();
		if (children != null) {
			for (Element child : children) {
				List<String> groups = child.mapChildren(el -> "group".equals(el.getName()),
														el -> el.getCData());
				RosterElement re = new RosterElement(
						JID.jidInstanceNS(child.getAttributeStaticStr("jid")),
						child.getAttributeStaticStr("name"),
						groups == null ? null : groups.toArray(String[]::new));
				re.setSubscription(subscriptionType(child));
				Optional.ofNullable(child.getChild("channel", "urn:xmpp:mix:roster:0"))
						.map(el -> el.getAttributeStaticStr("participant-id"))
						.ifPresent(re::setMixParticipantId);
				roster.add(re);
			}
		}
		String rosterStr = roster.stream()
				.map(RosterElement::getRosterElement)
				.map(Element::toString)
				.collect(Collectors.joining());
		UserRepository userRepository = repositoryHolder.getRepository(
				UserRepository.class, user.getDomain());
		if (rosterStr.isEmpty()) {
			userRepository.removeData(user, RosterAbstract.ROSTER);
		} else {
			userRepository.setData(user, RosterAbstract.ROSTER, rosterStr);
		}
		log.finest("importing user " + user + " roster...");
	}

	private static RosterAbstract.SubscriptionType subscriptionType(Element rosterItem) {
		return subscriptionType(rosterItem.getAttributeStaticStr("subscription"),
								rosterItem.getAttributeStaticStr("ask"),
								Boolean.parseBoolean(rosterItem.getAttributeStaticStr("approved")));
	}

	private static RosterAbstract.SubscriptionType subscriptionType(String subscr, String ask, boolean approved) {
		RosterAbstract.SubscriptionType type = RosterAbstract.SubscriptionType.none;
		if (subscr != null) {
			try {
				type = RosterAbstract.SubscriptionType.valueOf(subscr);
				boolean subscribe = "subscribe".equals(ask);
				if (approved) {
					type = switch (type) {
						case to -> RosterAbstract.SubscriptionType.to_pre_approved;
						case none -> {
							if (subscribe) {
								yield RosterAbstract.SubscriptionType.none_pending_out_pre_approved;
							}
							yield RosterAbstract.SubscriptionType.none_pre_approved;
						}
						default -> type;
					};
				}
				type = switch (type) {
					case none -> RosterAbstract.SubscriptionType.none_pending_out;
					case from -> RosterAbstract.SubscriptionType.from_pending_out;
					default -> type;
				};
			} catch (IllegalArgumentException ex) {
				// in this case, lets assume that subscription is `none`
			}
		}
		return type;
	}
	
	public void process(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			process(reader);
		}
	}

	public void process(BufferedReader reader) throws IOException {
		ImportXMLHandler importXMLHandler = new ImportXMLHandler(this);
		while (reader.ready()) {
			String data = reader.readLine();
			parser.parse(importXMLHandler, data);
		}
	}
}
