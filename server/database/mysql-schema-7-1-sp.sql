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

-- Database stored procedures and functions for Tigase schema version 5.1

source database/mysql-schema-5-1-sp.sql;

-- LOAD FILE: database/mysql-schema-5-1-sp.sql

-- QUERY START:
drop procedure if exists TigAddNode;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigPutDBProperty;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigUserLogout;
-- QUERY END:
 
 
delimiter //

-- QUERY START:
-- Helper procedure for adding a new node
create procedure TigAddNode(_parent_nid bigint, _uid bigint, _node varchar(255) CHARSET utf8)
begin
  if exists(SELECT 1 FROM tig_nodes WHERE parent_nid = _parent_nid AND uid = _uid AND node = _node)  then
    SELECT nid FROM tig_nodes WHERE parent_nid = _parent_nid AND uid = _uid AND node = _node;
  ELSEIF exists(SELECT 1 FROM tig_nodes WHERE _parent_nid is null AND uid = _uid AND 'root' = _node)  then
    SELECT nid FROM tig_nodes WHERE uid = _uid AND node = _node;
  ELSE
	insert into tig_nodes (parent_nid, uid, node) values (_parent_nid, _uid, _node);
	select LAST_INSERT_ID() as nid;
  END IF;
end //
-- QUERY END:

-- QUERY START:
-- Database properties set - procedure
create procedure TigPutDBProperty(_tkey varchar(255) CHARSET utf8, _tval mediumtext CHARSET utf8)
begin
  if exists( select 1 from tig_pairs, tig_users where
    (sha1_user_id = sha1(lower('db-properties'))) AND (tig_users.uid = tig_pairs.uid)
    AND (pkey = _tkey))
  then
    update tig_pairs tp, tig_users tu, tig_nodes tn set pval = _tval
    where (tu.sha1_user_id = sha1(lower('db-properties'))) AND (tu.uid = tp.uid)
      AND (tp.pkey = _tkey) AND (tn.node = "root");
  else
    insert into tig_pairs (pkey, pval, uid, nid)
          select _tkey, _tval, tu.uid, tn.nid from tig_users tu left join tig_nodes tn on tn.uid=tu.uid
        where (tu.sha1_user_id = sha1(lower('db-properties')) and tn.node="root");
  end if;
end //
-- QUERY END:


-- QUERY START:
-- It decreases online_status and sets last_logout time to the current timestamp
create procedure TigUserLogout(_user_id varchar(2049) CHARSET utf8)
begin
	update tig_users
		set online_status = greatest(online_status - 1, 0),
			last_logout = CURRENT_TIMESTAMP
		where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

delimiter ;



