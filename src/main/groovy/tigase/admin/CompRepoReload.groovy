/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */

/*

Reload a component repository:
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

AS:Description: Reload component repository
AS:CommandId: comp-repo-reload
AS:Component: vhost-man,ext
*/

package tigase.admin

import tigase.db.*
import tigase.db.comp.*

def repo = (ComponentRepository)comp_repo
repo.reload()

def result = "Reloaded items - " + repo.size() + ":\n"
def items = repo.allItems()
if (items.size() > 0) {
	items.each { result += "\n" + it.getKey() }
}

return result
