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
alter table tig_nodes DROP constraint tig_nodes_uid_fkey;
alter table tig_pairs DROP constraint tig_pairs_uid_fkey;
alter table tig_pairs DROP constraint tig_pairs_nid_fkey;
alter table tig_users DROP constraint tig_users_pkey;
alter table tig_nodes DROP constraint tig_nodes_pkey;
drop index user_id;
drop index tnode;
drop index node;
drop index pkey;

select NOW(), ' - Renaming old tig_pairs table for later convertion';
alter table tig_pairs rename to tig_pairs_old;
create index pkey_old on tig_pairs_old (pkey);
create index puid_old on tig_pairs_old (uid);
create index pnid_old on tig_pairs_old (nid);

select NOW(), ' - Renaming old tig_nodes table for later convertion';
alter table tig_nodes rename to tig_nodes_old;
create index nnid_old on tig_nodes_old (nid);
create index parent_nid_old on tig_nodes_old (parent_nid);
create index nuid_old on tig_nodes_old (uid);
create index node_old on tig_nodes_old (node);

select NOW(), ' - Renaming old tig_users table for later convertion';
alter table tig_users rename to tig_users_old;
create index uuid_old on tig_users_old (uid);
create index user_id_old on tig_users_old (user_id);

select NOW(), ' - Loading new database schema 4.0';
\i database/postgresql-schema-4.sql;

select NOW(), ' - Setting passwords encoding in the database';
-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
select TigPutDBProperty('password-encoding', 'PLAIN');

select NOW(), ' - Temporarly adding old_uid column to tig_users table';
alter table tig_users add column old_uid bigint;
create index old_uid on tig_users (old_uid);
select NOW(), ' - Temporarly adding old_nid column to tig_nodes table';
alter table tig_nodes add column old_nid bigint;
create index old_nid on tig_nodes (old_nid);

select NOW(), ' - Copying tig_users data to a new table';
insert into tig_users (user_id, old_uid)
	select user_id, uid from tig_users_old;

select NOW(), ' - Copying tig_nodes data to a new table';
insert into tig_nodes (uid, old_nid, node)
	select tig_users.uid, nid, node from tig_users, tig_nodes_old
		where (tig_nodes_old.uid = tig_users.old_uid);

select NOW(), ' - Updating parent_nids in the new tig_nodes table';
create temporary table temp_nodes as
	select tig_nodes.uid as new_uid, tig_nodes.nid as new_nid,
			tig_nodes_old.nid as old_nid, tig_nodes_old.parent_nid as old_parent_nid
		from tig_nodes, tig_nodes_old where tig_nodes.old_nid = tig_nodes_old.nid;

create index new_nid on temp_nodes (new_nid);
create index old_nid on temp_nodes (old_nid);

update tig_nodes
	set parent_nid = temp_nodes.new_nid from tig_nodes_old, temp_nodes
	where (tig_nodes.old_nid = tig_nodes_old.nid)
		AND (tig_nodes_old.parent_nid = temp_nodes.old_nid);

select NOW(), ' - Loading tig_pairs table from old one with new uids and nids';
insert into tig_pairs (nid, uid, pkey, pval)
	select tig_nodes.nid, tig_users.uid, pkey, pval
		from tig_pairs_old, tig_users, tig_nodes
		where (tig_pairs_old.uid = tig_users.old_uid)
			AND (tig_pairs_old.nid = tig_nodes.old_nid);

select NOW(), ' - Droping temporarly columns';
drop index old_uid;
alter table tig_users drop column old_uid;
drop index old_nid;
alter table tig_nodes drop column old_nid;

select NOW(), ' - Converting user passwords to a new format';
select TigUsers2Ver4Convert();

select NOW(), ' - All done, database ready to use!';
