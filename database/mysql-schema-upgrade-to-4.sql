-- Database schema upgrade for Tigase version 4.0.0

select 'Droping index for user_id column';
alter table tig_users drop index user_id;
select 'Resizing user_id column to 2049 characters to comply with RFC';
alter table tig_users modify user_id varchar(2049) not null;
select 'Creating a new index for user_id column for first 765 bytes of the field';
alter table tig_users add index user_id (user_id(765));
select 'Adding sha1_user_id column';
alter table tig_users	add column sha1_user_id char(128) not null;
select 'Adding user_pw column';
alter table tig_users	add column user_pw varchar(255) not null;
select 'Adding last_login column';
alter table tig_users	add column last_login timestamp DEFAULT 0;
select 'Adding last_logout column';
alter table tig_users	add column last_logout timestamp DEFAULT 0;
select 'Adding online_status column';
alter table tig_users	add column online_status int default 0;
select 'Adding failed_logins column';
alter table tig_users	add column failed_logins int default 0;
select 'Adding account_status column';
alter table tig_users	add column account_status int default 1;
select 'Creating a new index for user_pw column';
alter table tig_users	add index user_pw (user_pw);
select 'Creating a new index for last_login column';
alter table tig_users	add index last_login (last_login);
select 'Creating a new index for last_logout column';
alter table tig_users	add index last_logout (last_logout);
select 'Creating a new index for account_status column';
alter table tig_users	add index account_status (account_status);
select 'Creating a new index for online_status column';
alter table tig_users	add index online_status (online_status);


select 'Resizing node column to 255 characters';
alter table tig_nodes modify node varchar(255) not null;
select 'Changing pval column type to mediumtext';
alter table tig_pairs modify pval mediumtext;

select 'Loading stored procedures definitions';
source database/mysql-schema-4-sp.schema;

select 'Setting passwords encoding in the database';
-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
call TigPutDBProperty('password-encoding', 'PLAIN');

select 'Converting database to a new format';
call TigUsers2Ver4Convert();

select 'Creating a new index for sha1_user_id column';
alter table tig_users add unique index sha1_user_id (sha1_user_id);

select 'Setting schema version to 4.0';
call TigPutDBProperty('schema-version', '4.0');
select 'All done, database ready to use!';
