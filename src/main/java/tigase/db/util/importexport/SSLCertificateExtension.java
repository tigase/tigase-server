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

import tigase.io.repo.CertificateItem;
import tigase.io.repo.CertificateRepository;
import tigase.util.ui.console.CommandlineParameter;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.db.util.importexport.RepositoryManager.isSet;

public class SSLCertificateExtension extends RepositoryManagerExtensionBase {

	private static Logger log = Logger.getLogger(SSLCertificateExtension.class.getCanonicalName());

	private final CommandlineParameter EXPORT_SSL_CERTIFICATES = new CommandlineParameter.Builder(null,
	                                                                                            "include-ssl-certs").description(
					"Export SSL certificates")
			.type(Boolean.class)
			.requireArguments(false)
			.defaultValue("false")
			.build();

	private final CommandlineParameter IMPORT_SSL_CERTIFICATES = new CommandlineParameter.Builder(null,
	                                                                                            "include-ssl-certs").description(
			"Import SSL certificates").type(Boolean.class).requireArguments(false).defaultValue("false").build();

	@Override
	public Stream<CommandlineParameter> getExportParameters() {
		return Stream.of(EXPORT_SSL_CERTIFICATES);
	}

	@Override
	public Stream<CommandlineParameter> getImportParameters() {
		return Stream.of(IMPORT_SSL_CERTIFICATES);
	}

	@Override
	public void exportDomainData(String domain, Writer writer) throws Exception {
		if (isSet(EXPORT_SSL_CERTIFICATES)) {
			CertificateRepository repo = getRepository(CertificateRepository.class, "default");
			for (CertificateItem item : repo.allItems()) {
				Set<String> domains = item.getCertificateEntry().getAllDomains();
				if (domains.contains(domain) || domains.contains("*." + domain) || domains.stream().anyMatch(it -> {
					String suffix = "." + domain;
					return it.endsWith(suffix) && !it.substring(0, it.length() - suffix.length()).contains(".");
				})) {
					log.info(() -> "exporting domain " + domain + " SSL certificate with alias " + item.getAlias() + "...");
					writer.write(item.toElement().toString());
					writer.write("\n");
				}
			}
		}
	}
	@Override
	public ImporterExtension startImportDomainData(String domain, String name, Map<String, String> attrs)
			throws Exception {
		if (!CertificateItem.REPO_ITEM_ELEM_NAME.equals(name)) {
			return null;
		}
		return new ImporterExtension() {
			private final Element certEl;

			{
				certEl = new Element(name);
				certEl.setAttributes(attrs);
			}

			@Override
			public boolean handleElement(Element element) throws Exception {
				certEl.addChild(element);
				return true;
			}

			@Override
			public void elementCData(String cdata) throws Exception {
				certEl.addCData(cdata);
			}

			@Override
			public void close() throws Exception {
				if (isSet(IMPORT_SSL_CERTIFICATES)) {
					CertificateRepository certificateRepository = getRepository(CertificateRepository.class, "default");
					CertificateItem item = certificateRepository.getItemInstance();
					log.info(() -> "importing SSL certificate " + domain + " with alias " + item.getAlias() + "...");
					log.finest(() -> "importing SSL certificate " + domain + " with alias " + item.getAlias() + " from " + certEl.toStringPretty());
					item.initFromElement(certEl);
					certificateRepository.addItem(item);
				} else {
					log.info(() -> "skipping import of SSL certificate " + domain);
				}
			}
		};
	}

	@Override
	public void exportUserData(Path userDirPath, BareJID user, Writer writer) throws Exception {

	}
}
