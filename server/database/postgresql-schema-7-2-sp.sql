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

-- Database stored procedures and fucntions for Tigase schema version 5.1

\i database/postgresql-schema-7-1-sp.sql

-- LOAD FILE: database/postgresql-schema-7-1-sp.sql


-- QUERY START:
-- It sets last_login time to the current timestamp
create or replace function TigUpdateLoginTime(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
begin
	update tig_users
		set last_login = now()
		where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END: