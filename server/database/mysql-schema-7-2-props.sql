--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
--
--  This program is free software: you can redistribute it and/or modify
--  it under the terms of the GNU Affero General Public License as published by
--  the Free Software Foundation, either version 3 of the License.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU Affero General Public License for more details.
--
--  You should have received a copy of the GNU Affero General Public License
--  along with this program. Look for COPYING file in the top folder.
--  If not, see http://www.gnu.org/licenses/.
--
--

-- QUERY START:
-- This is a dummy user who keeps all the database-properties
call TigExecuteIfNot(
    (select count(1) from tig_users where user_id = 'db-properties'),
    "call TigAddUserPlainPw('db-properties', NULL)"
);
-- QUERY END:

select NOW(), ' - Setting schema version to 7.2';

-- QUERY START:
call TigPutDBProperty('schema-version', '7.2');
-- QUERY END:
