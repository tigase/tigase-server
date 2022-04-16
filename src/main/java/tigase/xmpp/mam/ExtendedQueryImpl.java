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
package tigase.xmpp.mam;

import java.util.Collection;
import java.util.Collections;

public class ExtendedQueryImpl
		extends QueryImpl
		implements ExtendedQuery {

	private String beforeId;
	private String afterId;
	private Collection<String> ids = Collections.emptyList();

	public ExtendedQueryImpl() {
	}

	@Override
	public String getBeforeId() {
		return beforeId;
	}

	@Override
	public void setBeforeId(String id) {
		beforeId = id;
	}

	@Override
	public String getAfterId() {
		return afterId;
	}

	@Override
	public void setAfterId(String id) {
		afterId = id;
	}

	@Override
	public Collection<String> getIds() {
		return ids;
	}

	@Override
	public void setIds(Collection<String> ids) {
		if (ids != null) {
			this.ids = ids;
		} else {
			this.ids = Collections.emptyList();
		}
	}
}
