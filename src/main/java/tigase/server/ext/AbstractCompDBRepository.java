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
package tigase.server.ext;

import tigase.db.DBInitException;
import tigase.db.comp.UserRepoRepository;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.AbstractMessageReceiver;
import tigase.xmpp.jid.BareJID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Oct 24, 2009 3:55:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class AbstractCompDBRepository
		extends UserRepoRepository<CompRepoItem> {

	public static final String ITEMS_IMPORT_FILE = "etc/externalComponentItems";

	private static final Logger log = Logger.getLogger(AbstractCompDBRepository.class.getCanonicalName());

	@Inject
	private AbstractMessageReceiver component;

	@ConfigField(desc = "ID of the external components group", alias = "external-components-group")
	private String extenalComponentsGroup;

	protected AbstractCompDBRepository(String extenalComponentsGroup) {
		this.extenalComponentsGroup = extenalComponentsGroup;
		this.autoReloadInterval = 30;
	}

	@Override
	public void destroy() {
		// Nothing to destroy here
	}

	@Override
	public String getConfigKey() {
		return CompRepoDefaults.getConfigKey();
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return CompRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public CompRepoItem getItemInstance() {
		return CompRepoDefaults.getItemInstance();
	}

	@Override
	public String getItemsListPKey() {
		return extenalComponentsGroup;
	}

	@Override
	public String getPropertyKey() {
		return CompRepoDefaults.getPropertyKey();
	}

	@Override
	public BareJID getRepoUser() {
		return CompRepoDefaults.getRepoUser();
	}

	@Deprecated
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do here
	}

	@Override
	public String validateItem(CompRepoItem item) {
		String result = super.validateItem(item);
		if (result == null) {
			result = item.validate();
		}
		return result;
	}

	@Override
	public void initialize() {
		loadItemsFromFile();
		super.initialize();
	}

	public void loadItemsFromFile() {
		File f = new File(ITEMS_IMPORT_FILE);
		if (f.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				reader.lines()
						.flatMap(list -> Arrays.stream(list.split(",")))
						.map(this::newItemFromPropertyString)
						.forEach(this::addItemNoStore);
				f.delete();
			} catch (IOException ex) {
				log.log(Level.WARNING, "could not load external component items from the import file", ex);
			}
		}
	}

	private CompRepoItem newItemFromPropertyString(String str) {
		CompRepoItem item = this.getItemInstance();
		item.initFromPropertyString(str);
		return item;
	}
}
