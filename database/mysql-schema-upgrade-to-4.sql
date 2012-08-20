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
--

-- Database schema upgrade for Tigase version 4.0.0

select NOW(), ' - Dropping foreign indexes';
-- QUERY START:
alter table tig_nodes DROP FOREIGN KEY tig_nodes_constr;
-- QUERY END:
-- QUERY START:
alter table tig_pairs DROP FOREIGN KEY tig_pairs_constr_1;
-- QUERY END:
-- QUERY START:
alter table tig_pairs DROP FOREIGN KEY tig_pairs_constr_2;
-- QUERY END:

select NOW(), ' - Renaming old tig_pairs table for later convertion';
-- QUERY START:
alter table tig_pairs rename to tig_pairs_old;
-- QUERY END:
select NOW(), ' - Renaming old tig_nodes table for later convertion';
-- QUERY START:
alter table tig_nodes rename to tig_nodes_old;
-- QUERY END:
-- QUERY START:
alter table tig_nodes_old add index parent_nid (parent_nid);
-- QUERY END:
-- QUERY START:
alter table tig_nodes_old add index uid (uid);
-- QUERY END:
select NOW(), ' - Renaming old tig_users table for later convertion';
-- QUERY START:
alter table tig_users rename to tig_users_old;
-- QUERY END:

select NOW(), ' - Loading new database schema 4.0';
-- LOAD SCHEMA:
source database/mysql-schema-4-schema.sql;

select NOW(), ' - Setting passwords encoding in the database';
-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
-- QUERY START:
call TigPutDBProperty('password-encoding', 'PLAIN');
-- QUERY END:

select NOW(), ' - Temporarly adding old_uid column to tig_users table';
-- QUERY START:
alter table tig_users add column old_uid bigint;
-- QUERY END:
-- QUERY START:
alter table tig_users add index old_uid (old_uid);
-- QUERY END:
select NOW(), ' - Temporarly adding old_nid column to tig_nodes table';
-- QUERY START:
alter table tig_nodes add column old_nid bigint;
-- QUERY END:
-- QUERY START:
alter table tig_nodes add index old_nid (old_nid);
-- QUERY END:

select NOW(), ' - Copying tig_users data to a new table';
-- QUERY START:
insert into tig_users (user_id, sha1_user_id, old_uid)
	select user_id, sha1(user_id), uid from tig_users_old;
-- QUERY END:

select NOW(), ' - Copying tig_nodes data to a new table';
-- QUERY START:
insert into tig_nodes (uid, old_nid, node)
	select tig_users.uid, nid, node from tig_users, tig_nodes_old
		where (tig_nodes_old.uid = tig_users.old_uid);
-- QUERY END:

select NOW(), ' - Updating parent_nids in the new tig_nodes table';
-- QUERY START:
create temporary table temp_nodes
	select tig_nodes.uid as new_uid, tig_nodes.nid as new_nid,
			tig_nodes_old.nid as old_nid, tig_nodes_old.parent_nid as old_parent_nid
		from tig_nodes, tig_nodes_old where tig_nodes.old_nid = tig_nodes_old.nid;
-- QUERY END:
-- QUERY START:

alter table temp_nodes add index new_nid (new_nid);
-- QUERY END:
-- QUERY START:
alter table temp_nodes add index old_nid (old_nid);
-- QUERY END:
-- QUERY START:

update tig_nodes, tig_nodes_old, temp_nodes
	set tig_nodes.parent_nid = temp_nodes.new_nid
	where (tig_nodes.old_nid = tig_nodes_old.nid)
		AND (tig_nodes_old.parent_nid = temp_nodes.old_nid);
-- QUERY END:

select NOW(), ' - Loading tig_pairs table from old one with new uids and nids';
-- QUERY START:
insert into tig_pairs (nid, uid, pkey, pval)
	select tig_nodes.nid, tig_users.uid, pkey, pval
		from tig_pairs_old, tig_users, tig_nodes
		where (tig_pairs_old.uid = tig_users.old_uid)
			AND (tig_pairs_old.nid = tig_nodes.old_nid);
-- QUERY END:

select NOW(), ' - Droping temporarly columns';
-- QUERY START:
alter table tig_users drop index old_uid;
-- QUERY END:
-- QUERY START:
alter table tig_users drop column old_uid;
-- QUERY END:
-- QUERY START:
alter table tig_nodes drop index old_nid;
-- QUERY END:
-- QUERY START:
alter table tig_nodes drop column old_nid;
-- QUERY END:

select NOW(), ' - Converting user passwords to a new format';
-- QUERY START:
call TigUsers2Ver4Convert();
-- QUERY END:

select NOW(), ' - All done, database ready to use!';
