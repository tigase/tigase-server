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
import tigase.util.datetime.TimestampHelper;
import tigase.util.ui.console.CommandlineParameter;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostJDBCRepository;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.impl.Privacy;
import tigase.xmpp.impl.VCardTemp;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.jid.BareJID;

import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class Exporter {

	private static final Logger log = Logger.getLogger(Exporter.class.getSimpleName());
	private static final TimestampHelper TIMESTAMP_FORMATTER = new TimestampHelper();

	public static final CommandlineParameter EXPORT_MAM_SINCE = new CommandlineParameter.Builder(null, "export-mam-since").description("Export MAM archive since").type(
			LocalDateTime.class).required(false).build();
	public static final CommandlineParameter EXPORT_MAM_BATCH_SIZE = new CommandlineParameter.Builder(null,
																									  "export-mam-batch-size").description(
			"Export MAM archive batch size").type(Integer.class).defaultValue("50000").required(false).build();

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	public static Optional<Date> getExportMAMSinceValue() {
		return EXPORT_MAM_SINCE.getValue().map(str -> {
			try {
				return TIMESTAMP_FORMATTER.parseTimestamp(str);
			} catch (Exception ex) {
				try {
					LocalDateTime ts =  parseLocalDate(str);
					return Date.from(ts.toInstant(ZoneOffset.UTC));
				} catch (Exception ex2) {
					throw new RuntimeException("Could not parse " + str + " as timestamp", ex);
				}
			}
		});
	}

	public static Integer getExportMAMBatchSize() {
		return EXPORT_MAM_BATCH_SIZE.getValue().map(Integer::parseInt).orElse(50000);
	}

	private static LocalDateTime parseLocalDate(String str) throws Exception {
		try {
			return LocalDateTime.parse(str, DATETIME_FORMAT);
		} catch (Exception ex1) {
			return LocalDateTime.of(LocalDate.parse(str, DATE_FORMAT), LocalTime.MIDNIGHT);
		}
	}

	public static Date parseTimestamp(String str) {
		try {
			return TIMESTAMP_FORMATTER.parseTimestamp(str);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void exportInclude(Writer parentWriter, Path rootPath, Path filePath, RepositoryManager.ThrowingConsumer<Writer> writerConsumer) throws Exception {
		Files.createDirectories(filePath.getParent());
		parentWriter.append("<xi:include href='").append(rootPath.relativize(filePath).toString()).append("'/>\n");
		openXmlFile(filePath, writerConsumer);
	}

	public static void openXmlFile(Path filePath, RepositoryManager.ThrowingConsumer<Writer> writerConsumer) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
			writer.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			writerConsumer.accept(writer);
		}
	}

	private final RepositoryHolder repositoryHolder;
	private final VHostJDBCRepository vhostRepository;
	private final List<RepositoryManagerExtension> extensions;
	private final Path rootPath;
	
	public Exporter(RepositoryHolder repositoryHolder, VHostJDBCRepository vhostRepository, List<RepositoryManagerExtension> extensions, Path rootPath) {
		this.repositoryHolder = repositoryHolder;
		this.vhostRepository = vhostRepository;
		this.extensions = extensions;
		this.rootPath = rootPath;

		getExportMAMSinceValue().ifPresent(date -> log.info("exporting MAM since: " + date));
		log.info("exporting MAM in batch size of " + getExportMAMBatchSize() + " messages");
	}

	public void export(String fileName) throws Exception {
		openXmlFile(rootPath.resolve(fileName), rootWriter -> {
			rootWriter.append("<server-data xmlns='urn:xmpp:pie:0' xmlns:xi='http://www.w3.org/2001/XInclude'>\n");
			for (VHostItem item : vhostRepository.allItems()) {
				if ("default".equals(item.getKey())) {
					continue;
				}
				Path domainFileName = rootPath.resolve(item.getKey() + ".xml");
				exportInclude(rootWriter, rootPath, domainFileName,
							  domainWriter -> exportDomain(domainFileName, item.getKey(), domainWriter));
			}
			rootWriter.append("</server-data>");
		});
	}

	protected void exportDomain(Path domainFilePath, String domain, Writer writer) throws Exception {
		log.info("exporting domain " + domain + " data...");
		writer.append("<host xmlns='urn:xmpp:pie:0' xmlns:xi='http://www.w3.org/2001/XInclude' jid=\"")
				.append(XMLUtils.escape(domain))
				.append("\">\n");

		// exporting domain users
		UserRepository userRepository = repositoryHolder.getRepository(UserRepository.class, domain);
		List<BareJID> users = userRepository.getUsers();
		int exportedUsers = 0;
		for (BareJID user : users) {
			if (user.getLocalpart() == null || !domain.equals(user.getDomain())) {
				continue;
			}

			Path userFilePath = domainFilePath.resolveSibling(domain).resolve(user.getLocalpart() + ".xml");
			exportInclude(writer, rootPath, userFilePath, userWriter -> exportUser(userRepository, userFilePath, user, userWriter));
			exportedUsers++;
		}

		// exporting additional data, ie. components running on subdomains...
		for (RepositoryManagerExtension extension : extensions) {
			extension.exportDomainData(domain, writer);
		}

		log.info("exported domain " + domain + " with " + exportedUsers + " users");

		writer.append("</host>");
	}

	protected void exportUser(UserRepository userRepository, Path userFilePath, BareJID user, Writer writer) throws Exception {
		log.info("exporting user " + user + " data...");
		writer.append("<user xmlns='urn:xmpp:pie:0' xmlns:xi='http://www.w3.org/2001/XInclude' name=\"")
				.append(XMLUtils.escape(user.getLocalpart()))
				.append("\"");


		AuthRepository authRepository = repositoryHolder.getRepository(AbstractAuthRepositoryWithCredentials.class, user.getDomain());
		try {
			AuthRepository.AccountStatus accountStatus = authRepository.getAccountStatus(user);
			writer.append(" xmlns:tigase=\"tigase:xep-0227:user:0\"");
			writer.append(" tigase:status=\"").append(accountStatus.name()).append("\"");
		} catch (Exception ex) {
			log.severe("error for " + user + " using " + authRepository.getClass().getSimpleName());
			throw ex;
		}

		writer.append(">");
		// roster
		String rosterStr = userRepository.getData(user, null, RosterAbstract.ROSTER, null);
		if (rosterStr != null) {
			Map<BareJID, RosterElement> roster = new LinkedHashMap<>();
			RosterFlat.parseRosterUtil(rosterStr, roster, null);
			writer.append("<query xmlns='jabber:iq:roster'>");
			for (RosterElement re : roster.values()) {
				Element rosterItem = re.getRosterItem();
				if (re.getMixParticipantId() != null) {
					rosterItem.addChild(new Element("channel", new String[] { "xmlns", "participant-id" }, new String[] { "urn:xmpp:mix:roster:0", re.getMixParticipantId() }));
				}
				writer.append(rosterItem.toString());
			}
			writer.append("</query>");
		}
		// vcard-temp
		String vcardTemp = userRepository.getData(user, "public/vcard-temp", VCardTemp.VCARD_KEY);
		if (vcardTemp != null) {
			writer.append(vcardTemp);
		}
		// privacy lists
		String defListName = userRepository.getData(user, Privacy.PRIVACY, Privacy.DEFAULT);
		String[] subnodes = userRepository.getSubnodes(user, Privacy.PRIVACY);
		if (subnodes != null && subnodes.length > 0) {
			writer.append("<query xmlns='jabber:iq:privacy'>");
			if (defListName != null) {
				writer.append("<default name=\"").append(XMLUtils.escape(defListName)).append("\"/>");
				for (String node : subnodes) {
					String list = userRepository.getData(user, Privacy.PRIVACY + "/" + node, Privacy.PRIVACY_LIST);
					if (list != null) {
						writer.append(list);
					}
				}
			}
			writer.append("</query>");
		}

		Path userDirPath = userFilePath.getParent().resolve(user.getLocalpart());
		for (RepositoryManagerExtension extension : extensions) {
			extension.exportUserData(userDirPath, user, writer);
		}

		writer.append("</user>");
	}

}
