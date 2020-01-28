/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.pubsub.PubSubComponent;
import tigase.xml.Element;

import java.util.List;

@Bean(name = "mix", parent = Kernel.class, active = true)
@ConfigType(ConfigTypeEnum.DefaultMode)
public class MixComponent extends PubSubComponent {

	@Override
	public List<Element> getDiscoFeatures() {
		return super.getDiscoFeatures();
	}

	@Override
	public String getDiscoCategory() {
		return "conference";
	}

	@Override
	public String getDiscoCategoryType() {
		return "mix";
	}

	@Override
	public String getDiscoDescription() {
		return "Mediated Information eXchange";
	}
}
