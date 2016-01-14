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

\i database/postgresql-schema-5-1-sp.sql

-- LOAD FILE: database/postgresql-schema-5-1-sp.sql


-- QUERY START:
-- Database properties set - procedure
create or replace function TigPutDBProperty(varchar(255), text) returns void as '
declare
  _tkey alias for $1;
  _tval alias for $2;
begin
  if exists( select pval from tig_pairs, tig_users where
		(lower(user_id) = lower(''db-properties'')) AND (tig_users.uid = tig_pairs.uid)
		AND (pkey = _tkey))
  then
	  update tig_pairs set pval = _tval from tig_users
      where (lower(tig_users.user_id) = lower(''db-properties''))
        AND (tig_users.uid = tig_pairs.uid)
        AND (pkey = _tkey);
  else
    insert into tig_pairs (pkey, pval, uid, nid)
		  select _tkey, _tval, tu.uid, tn.nid from tig_users tu  left join tig_nodes tn on tn.uid=tu.uid
			  where (lower(user_id) = lower(''db-properties'')  and tn.node=''root'' );
  end if;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END: