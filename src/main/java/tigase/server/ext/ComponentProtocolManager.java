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

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.ComponenScriptCommandProcessor;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.db.comp.ComponentRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.DisableDisco;

import javax.script.Bindings;

@Bean(name="ext-man", parent = Kernel.class, active = false)
@ConfigType({ConfigTypeEnum.DefaultMode})
public class ComponentProtocolManager extends AbstractKernelBasedComponent implements DisableDisco {

	@Inject
	private CompCompDBRepository repo;

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}
	
	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	@Override
	public String getDiscoDescription() {
		return "External Component Manager";
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(CompCompDBRepository.class).setActive(true).exec();
		kernel.registerBean(DiscoveryModule.class).setActive(true).exec();
		kernel.registerBean(ComponenScriptCommandProcessor.class).setActive(true).exec();
		kernel.registerBean(AdHocCommandManager.class).setActive(true).exec();
		kernel.registerBean(AdHocCommandModule.class).setActive(true).exec();
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
		binds.put("kernel", kernel);
	}

	@Override
	public void initialize() {
		super.initialize();
	}
}
