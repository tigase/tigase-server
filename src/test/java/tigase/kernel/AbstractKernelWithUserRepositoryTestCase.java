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
package tigase.kernel;

import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.db.xml.XMLRepository;
import tigase.kernel.core.Kernel;

/**
 * Class is a base class for tests requiring instances of Kernel and User/Auth repositories.
 */
public class AbstractKernelWithUserRepositoryTestCase extends AbstractKernelTestCase {

	private XMLRepository repository;

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		try {
			String xmlRepositoryURI = "memory://xmlRepo?autoCreateUser=true";
			repository = new XMLRepository();
			repository.initRepository(xmlRepositoryURI, null);
			kernel.registerBean("userAuthRepository").asInstance(repository).exportable().exec();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize user/auth repository", ex);
		}
	}

	public UserRepository getUserRepository() {
		return repository;
	}

	public AuthRepository getAuthRepository() {
		return repository;
	}
}
