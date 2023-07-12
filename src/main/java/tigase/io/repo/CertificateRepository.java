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

package tigase.io.repo;

import tigase.annotations.TigaseDeprecated;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.db.comp.UserRepoRepository;
import tigase.io.CertificateContainer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.jid.BareJID;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "repository", parent = CertificateContainer.class, active = false)
public class CertificateRepository
		extends UserRepoRepository<CertificateItem> {

	private static final Logger log = Logger.getLogger(CertificateRepository.class.getName());

	private final static String CONFIG_KEY = "vhost-certificates";

	private final static BareJID REPO_USER_JID = BareJID.bareJIDInstanceNS("certificate-manager");

	@ConfigField(desc = "Automatically migrate certificates from filesystem to repository (and make backup)", alias = "move-from-filesystem-to-repository")
	protected boolean moveFromFilesystemToRepository = true;

	public CertificateRepository() {
		this.autoReloadInterval = TimeUnit.HOURS.toSeconds(1);
	}

	@Override
	public void addItem(CertificateItem item) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding item: {0}", item);
		}
		addItemNoStore(item);

		// store only single item for performance
		if (repo != null && isInitialized()) {
			try {
				repo.setData(getRepoUser(), getItemsListPKey(), item.getKey(), item.toElement().toString());
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error storing item in the repository", e);
			}
		}
	}

	@Override
	public boolean itemChanged(CertificateItem oldItem, CertificateItem newItem) {
		return oldItem.getFingerprint().isPresent() && newItem.getFingerprint().isPresent() &&
				oldItem.getFingerprint().get().equals(newItem.getFingerprint().get());
	}

	@Override
	public void destroy() {
	}

	@Override
	public String getConfigKey() {
		return CONFIG_KEY;
	}

	@Override
	public CertificateItem getItemInstance() {
		return new CertificateItem();
	}

	@Override
	public String getPropertyKey() {
		return "--" + CONFIG_KEY;
	}

	@Override
	public BareJID getRepoUser() {
		return REPO_USER_JID;
	}

	public boolean isMoveFromFilesystemToRepository() {
		return moveFromFilesystemToRepository;
	}

	@Override
	public void reload() {
		int hashCode = 0;
		try {
			if (repo == null) {
				log.log(Level.SEVERE, "Repository is not initialised - skipping reload");
				return;
			}

			final Map<String, String> itemsMap = repo.getDataMap(getRepoUser(), getItemsListPKey());

			if (itemsMap != null) {
				hashCode = itemsMap.hashCode();

				if (hashCode != itemsHash) {
					Set<String> oldKeys = new HashSet<>(super.items.keySet());

					itemsMap.forEach((key, value) -> parseElement(value).ifPresentOrElse(item -> {
						addItemNoStore(item);
						oldKeys.remove(item.getKey());
					}, () -> oldKeys.remove(key)));
					log.log(Level.CONFIG, "All loaded items: {0}", items);

					itemsHash = hashCode;

					oldKeys.forEach(this::removeItemNoStore);
				}
			}
		} catch (TigaseDBException ex) {
			log.log(Level.SEVERE, "Problem with loading items list from the database.", ex);
		}
	}

	@Override
	public void removeItem(String key) {
		super.removeItem(key);

		if (repo != null && isInitialized()) {
			try {
				repo.removeData(getRepoUser(), getItemsListPKey(), key);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error removing item from the repository", e);
			}
		}
	}

	@Override
	public void store() {
		if (repo != null && isInitialized()) {
			for (CertificateItem item : items.values()) {
				try {
					repo.setData(getRepoUser(), getItemsListPKey(), item.getKey(), item.toElement().toString());
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error storing items list in the repository", e);
				}
			}
		}
	}

	private Optional<CertificateItem> parseElement(String element) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		SimpleParser parser = SingletonFactory.getParserInstance();

		parser.parse(domHandler, element);

		Queue<Element> parsedElements = domHandler.getParsedElements();

		if (!parsedElements.isEmpty() && parsedElements.peek() != null) {
			CertificateItem item = getItemInstance();
			item.initFromElement(parsedElements.peek());
			return Optional.of(item);
		} else {
			log.log(Level.WARNING, "Parsing certificate from element failed: " + element);
			return Optional.empty();
		}
	}
}
