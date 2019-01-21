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

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Converter;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;

@Bean(name = "bean8", active = true)
public class Bean8 {

	@Inject(nullAllowed = false)
	private Bean6 bean6;

	@ConfigField(desc = "Field with string value")
	@Converter(converter = KernelTest.CustomTypesConverter.class)
	private String sample;

	public Bean6 getBean6() {
		return bean6;
	}

	public void setBean6(Bean6 bean6) {
		this.bean6 = bean6;
	}

	public String getSample() {
		return sample;
	}

}
