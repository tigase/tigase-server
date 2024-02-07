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

import tigase.db.UserRepository;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class JabberIqPrivateExtension extends RepositoryManagerExtensionBase {

	private static final Logger log = Logger.getLogger(JabberIqPrivateExtension.class.getSimpleName());

	@Override
	public void exportDomainData(String domain, Writer writer) throws Exception {
		// nothing to do...
	}

	@Override
	public void exportUserData(Path userDirPath, BareJID user, Writer writer) throws Exception {
		UserRepository userRepository = getRepository(UserRepository.class, user.getDomain());
		String[] keys = userRepository.getKeys(user, "jabber:iq:private");
		if (keys != null) {
			writer.append("<query xmlns='jabber:iq:private'>");
			for (String key : keys) {
				String data = userRepository.getData(user, key);
				if (data != null) {
					writer.append(data);
				}
			}
			writer.append("</query>");
		}
	}

	@Override
	public ImporterExtension startImportUserData(BareJID userJid, String name,
												 Map<String, String> attrs) throws Exception {
		if ("query".equals(name) && "jabber:iq:private".equals(attrs.get("xmlns"))) {
			log.finest("importing user " + userJid + " jabber:iq:private...");
			return new JabberIqPrivateImporterExtension(getRepository(UserRepository.class, userJid.getDomain()), userJid);
		}
		return null;
	}

	public static class JabberIqPrivateImporterExtension extends AbstractImporterExtension {

		private final UserRepository userRepository;
		private final BareJID user;

		public JabberIqPrivateImporterExtension(UserRepository userRepository, BareJID user) {
			this.userRepository = userRepository;
			this.user = user;
		}

		@Override
		public boolean handleElement(Element item) throws Exception {
			userRepository.setData(user, "jabber:iq:private", item.getName() + item.getXMLNS(),
					 item.toString());
			return true;
		}
	}
}
