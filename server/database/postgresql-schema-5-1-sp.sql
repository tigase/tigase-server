--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $

-- Database stored procedures and fucntions for Tigase schema version 5.1

\i database/postgresql-schema-4-sp.sql

-- LOAD FILE: database/postgresql-schema-4-sp.sql

-- QUERY START:
create or replace function TigUpdatePairs(bigint, bigint, varchar(255), text) returns void as '
declare
  _nid alias for $1;
  _uid alias for $2;
  _tkey alias for $3;
  _tval alias for $4;
begin
  if exists(select 1 from tig_pairs where nid = _nid and uid = _uid and pkey = _tkey)
  then
        update tig_pairs set pval = _tval where nid = _nid and uid = _uid and pkey = _tkey;
  else
        insert into tig_pairs (nid, uid, pkey, pval) values (_nid, _uid, _tkey, _tval);
  end if;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END: