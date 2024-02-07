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
package tigase.xmpp.mam.util;

import tigase.db.util.importexport.Exporter;
import tigase.server.Message;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;
import tigase.xmpp.mam.MAMItemHandler;
import tigase.xmpp.mam.MAMRepository;
import tigase.xmpp.mam.Query;

import java.io.Writer;
import java.util.Date;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MAMRepositoryManagerExtensionHelper {

	private static Logger log = Logger.getLogger(MAMRepositoryManagerExtensionHelper.class.getCanonicalName());

	public static void exportDataFromRepository(MAMRepository mamRepository, BareJID repoJID, BareJID askingJID, Writer archiveWriter) throws Exception {
		exportDataFromRepository(mamRepository, repoJID, askingJID, null, archiveWriter);
	}
	public static void exportDataFromRepository(MAMRepository mamRepository, BareJID repoJID, BareJID askingJID, BiConsumer<MAMRepository.Item,Element> outputModifier, Writer archiveWriter) throws Exception {
		archiveWriter.append("<archive xmlns='urn:xmpp:pie:0#mam' xmlns:xi='http://www.w3.org/2001/XInclude'>");
		Query query = mamRepository.newQuery(repoJID);
		query.setComponentJID(JID.jidInstance(repoJID));
		query.setQuestionerJID(JID.jidInstance(askingJID));
		query.setXMLNS("urn:xmpp:mam:2");
		Exporter.getExportMAMSinceValue().ifPresent(query::setStart);
		query.getRsm().setMax(Integer.MAX_VALUE);
		mamRepository.queryItems(query, new MAMItemHandler() {
			@Override
			public void itemFound(Query query, MAMRepository.Item item) {
				Element result = this.prepareResult(query, item);
				if (result != null) {
					if (outputModifier != null) {
						outputModifier.accept(item, result);
					}
					try {
						archiveWriter.append(result.toString());
					} catch (Throwable ex) {
						log.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}
		});
		archiveWriter.append("</archive>");
	}

	public abstract static class AbstractImporterExtension extends tigase.db.util.importexport.AbstractImporterExtension {

		public boolean handleElement(Element element) throws Exception {
			if (!"result".equals(element.getName())) {
				return false;
			}
			String stanzaId = Optional.ofNullable(element.getAttributeStaticStr("id")).orElseThrow();
			Element forwardedEl = Optional.ofNullable(element.findChild(
							el -> "forwarded".equals(el.getName()) && "urn:xmpp:forward:0".equals(el.getXMLNS())))
					.orElseThrow();
			Element messageEl = Optional.ofNullable(forwardedEl.findChild(el -> "message".equals(el.getName())))
					.orElseThrow();
			Date timestamp = Optional.ofNullable(
							forwardedEl.findChild(el -> "delay".equals(el.getName()) && "urn:xmpp:delay".equals(el.getXMLNS())))
					.map(delay -> delay.getAttributeStaticStr("stamp"))
					.map(this::parseTimestamp)
					.orElseThrow();

			Message message = new Message(messageEl);

			return handleMessage(message, stanzaId, timestamp, element);
		}

		protected abstract boolean handleMessage(Message message, String stableId, Date timestamp, Element source) throws Exception;
	}
}
