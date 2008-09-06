-- Database schema upgrade for Tigase version 4.0.0

alter table tig_pairs rename to tig_pairs_old;
alter table tig_nodes rename to tig_nodes_old;
alter table tig_users rename to tig_users_old;

select 'Loading stored procedures definitions';
source database/mysql-schema-4.sql;

select 'Setting passwords encoding in the database';
-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
call TigPutDBProperty('password-encoding', 'PLAIN');

select 'Converting database to a new format';
call TigUsers2Ver4Convert();

select 'Setting schema version to 4.0';
call TigPutDBProperty('schema-version', '4.0');
select 'All done, database ready to use!';
