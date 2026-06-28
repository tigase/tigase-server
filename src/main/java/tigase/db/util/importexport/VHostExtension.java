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

import tigase.util.ui.console.CommandlineParameter;
import tigase.vhosts.VHostComponentRepository;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostItemImpl;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.db.util.importexport.RepositoryManager.isSet;

public class VHostExtension
		extends RepositoryManagerExtensionBase {

	private static Logger log = Logger.getLogger(VHostExtension.class.getCanonicalName());

	private final CommandlineParameter EXPORT_VHOST_SETTINGS = new CommandlineParameter.Builder(null,
	                                                                                               "include-vhosts").description(
					"Export virtual hosts settings (if any exist)")
			.type(Boolean.class)
			.requireArguments(false)
			.defaultValue("false")
			.build();

	private final CommandlineParameter IMPORT_VHOST_SETTINGS = new CommandlineParameter.Builder(null,
	                                                                                               "include-vhosts").description(
			"Import virtual hosts settings").type(Boolean.class).requireArguments(false).defaultValue("false").build();

	@Override
	public Stream<CommandlineParameter> getExportParameters() {
		return Stream.of(EXPORT_VHOST_SETTINGS);
	}

	@Override
	public Stream<CommandlineParameter> getImportParameters() {
		return Stream.of(IMPORT_VHOST_SETTINGS);
	}

	@Override
	public void exportDomainData(String domain, Writer writer) throws Exception {
		if (isSet(EXPORT_VHOST_SETTINGS)) {
			VHostComponentRepository vhostRepository = getRepository(VHostComponentRepository.class, "default");
			VHostItem item = vhostRepository.getItem(domain);
			if (item != null) {
				log.info(() -> "exporting domain " + domain + " vhost configuration...");
				writer.append(item.toElement().toString());
				writer.write("\n");
			}
		}
	}

	@Override
	public ImporterExtension startImportDomainData(String domain, String name, Map<String, String> attrs)
			throws Exception {
		if (!VHostItemImpl.VHOST_ELEM.equals(name)) {
			return null;
		}

		return new ImporterExtension() {
			private final Element vhostEl;

			{
				vhostEl = new Element(name);
				vhostEl.setAttributes(attrs);
			}

			@Override
			public boolean handleElement(Element element) throws Exception {
				vhostEl.addChild(element);
				return true;
			}

			@Override
			public void elementCData(String cdata) throws Exception {
				vhostEl.addCData(cdata);
			}

			@Override
			public void close() throws Exception {
				if (isSet(IMPORT_VHOST_SETTINGS)) {
					log.info(() -> "updating vhost " + domain + " configuration...");
					log.finest(() -> "updating vhost " + domain + " configuration from " + vhostEl.toStringPretty());
					VHostComponentRepository vhostRepository = getRepository(VHostComponentRepository.class, "default");
					VHostItem item = vhostRepository.getItemInstance();
					item.initFromElement(vhostEl);
					vhostRepository.addItem(item);
				} else {
					log.info(() -> "skipping vhost " + domain + " configuration update...");
				}
			}
		};
	}

	@Override
	public void exportUserData(Path userDirPath, BareJID user, Writer writer) throws Exception {

	}
}
