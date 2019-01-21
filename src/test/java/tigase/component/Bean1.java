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
package tigase.component;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.jid.JID;

/**
 * Created by bmalkow on 19.10.2015.
 */
@Bean(name = "bean1", active = true)
public class Bean1 {

	private String field1;
	@ConfigField(desc = "Field 02")
	private int field2;
	@ConfigField(desc = "Field 03", alias = "alias1")
	private JID field3;
	@ConfigField(desc = "Field 04", alias = "alias2")
	private JID field4;

	public String getField1() {
		return field1;
	}

	public Bean1 setField1(String field1) {
		this.field1 = field1;
		return this;
	}

	public int getField2() {
		return field2;
	}

	public Bean1 setField2(int field2) {
		this.field2 = field2;
		return this;
	}

	public JID getField3() {
		return field3;
	}

	public Bean1 setField3(JID field3) {
		this.field3 = field3;
		return this;
	}

	public JID getField4() {
		return field4;
	}

	public Bean1 setField4(JID field4) {
		this.field4 = field4;
		return this;
	}

}
