--
-- Tigase XMPP Server - The instant messaging server
-- Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program. Look for COPYING file in the top folder.
-- If not, see http://www.gnu.org/licenses/.
--

-- QUERY START: create database
create database ${dbName};
-- QUERY END: create database

-- QUERY START: add user
do $$
begin
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${dbUser}') THEN
	create user ${dbUser} with password '${dbPass}';
end if;
end$$;
-- QUERY END: add user

-- QUERY START: GRANT ALL
GRANT ALL ON database ${dbName} TO ${dbUser};
-- QUERY END: GRANT ALL

-- QUERY START: ALTER DATABASE
ALTER DATABASE ${dbName} OWNER TO ${dbUser};
-- QUERY END: ALTER DATABASE



