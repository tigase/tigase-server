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

import tigase.db.comp.UserRepoRepository;
import tigase.io.CertificateContainer;
import tigase.kernel.beans.Bean;
import tigase.xmpp.jid.BareJID;

import java.util.concurrent.TimeUnit;

@Bean(name = "repository", parent = CertificateContainer.class, active = true)
public class CertificateRepository
		extends UserRepoRepository<CertificateItem> {

	private final static String CONFIG_KEY = "vhost-certificates";

	private final static BareJID REPO_USER_JID = BareJID.bareJIDInstanceNS("certificate-manager");

	public CertificateRepository() {
		this.autoReloadInterval = TimeUnit.MINUTES.toSeconds(60);
	}

	@Override
	public void destroy() {
	}

	@Override
	public CertificateItem getItemInstance() {
		return new CertificateItem();
	}

	@Override
	public String getConfigKey() {
		return CONFIG_KEY;
	}

	@Override
	public String getPropertyKey() {
		return "--" + CONFIG_KEY;
	}

	@Override
	public BareJID getRepoUser() {
		return REPO_USER_JID;
	}

	@Override
	public void reload() {
		super.reload();
	}
}
