-- Database schema upgrade for Tigase version 4.0.0

select NOW(), ' - Copying old tig_pairs table for later convertion';
alter table tig_pairs rename to tig_pairs_old;
select NOW(), ' - Copying old tig_nodes table for later convertion';
alter table tig_nodes rename to tig_nodes_old;
select NOW(), ' - Copying old tig_users table for later convertion';
alter table tig_users rename to tig_users_old;

select NOW(), ' - Loading new database schema 4.0';
source database/mysql-schema-4.sql;

select NOW(), ' - Setting passwords encoding in the database';
-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
call TigPutDBProperty('password-encoding', 'PLAIN');

select NOW(), ' - Temporarly adding old_uid column to tig_users table';
alter table tig_users add column old_uid bigint;
alter table tig_users add index old_uid (old_uid);
select NOW(), ' - Temporarly adding old_nid column to tig_nodes table';
alter table tig_nodes add column old_nid bigint;
alter table tig_nodes add index old_nid (old_nid);

select NOW(), ' - Copying tig_users data to a new table';
insert into tig_users (user_id, sha1_user_id, old_uid)
	select user_id, sha1(user_id), uid from tig_users_old;

select NOW(), ' - Copying tig_nodes data to a new table';
insert into tig_nodes (uid, old_nid, node)
	select tig_users.uid, nid, node from tig_users, tig_nodes_old
		where (tig_nodes_old.uid = tig_users.old_uid);
select NOW(), ' - Updating parent_nids in the new tig_nodes table';
update tig_nodes, tig_nodes_old set tig_nodes.parent_nid = tig_nodes.nid
	where (tig_nodes_old.parent_nid = tig_nodes.old_nid);

select NOW(), ' - Loading tig_pairs table from old one with new uids and nids';
insert into tig_pairs (nid, uid, pkey, pval)
	select tig_nodes.nid, tig_users.uid, pkey, pval
		from tig_pairs_old, tig_users, tig_nodes
		where (tig_pairs_old.uid = tig_users.old_uid)
			AND (tig_pairs_old.nid = tig_nodes.old_nid);

select NOW(), ' - Droping temporarly columns';
alter table tig_users drop index old_uid;
alter table tig_users drop column old_uid;
alter table tig_nodes drop index old_nid;
alter table tig_nodes drop column old_nid;

select NOW(), ' - Converting user passwords to a new format';
call TigUsers2Ver4Convert();

select NOW(), ' - Setting schema version to 4.0';
call TigPutDBProperty('schema-version', '4.0');
select NOW(), ' - All done, database ready to use!';
