--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
--
--  This program is free software: you can redistribute it and/or modify
--  it under the terms of the GNU General Public License as published by
--  the Free Software Foundation, either version 3 of the License.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU General Public License for more details.
--
--  You should have received a copy of the GNU General Public License
--  along with this program. Look for COPYING file in the top folder.
--  If not, see http://www.gnu.org/licenses/.
--
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $
--

--  To load schema to MySQL database execute following commands:
--
--  mysqladmin -u root -pdbpass create tigase
--  mysql -u root -pdbpass tigase < database/mysql-schema.sql
--  echo "GRANT ALL ON tigase.* TO tigase_user@'%' \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql
--  echo "GRANT ALL ON tigase.* TO tigase_user@'localhost' \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql
--  echo "GRANT ALL ON tigase.* TO tigase_user \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql


create table short_news (
  -- Automatic record ID
  snid            bigint unsigned NOT NULL auto_increment,
  -- Automaticly generated timestamp and automaticly updated on change
  publishing_time timestamp,
	-- Optional news type: 'shorts', 'minis', 'techs', 'funs'....
	news_type       varchar(10),
  -- Author JID
  author          varchar(128) NOT NULL,
  -- Short subject - this is short news, right?
  subject         varchar(128) NOT NULL,
  -- Short news message - this is short news, right?
  body            varchar(1024) NOT NULL,
  primary key(snid),
  key publishing_time (publishing_time),
  key author (author),
  key news_type (news_type)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table xmpp_stanza (
			 id bigint unsigned NOT NULL auto_increment,
			 stanza text NOT NULL,

			 primary key (id)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table tig_users (
       uid bigint unsigned NOT NULL auto_increment,

			 -- Jabber User ID
       user_id varchar(2049) NOT NULL,
			 -- UserID SHA1 hash to prevent duplicate user_ids
			 sha1_user_id char(128) NOT NULL,
			 -- User password encrypted or not
			 user_pw varchar(255) NOT NULL,
			 -- Time of the last user login
			 last_login timestamp DEFAULT 0,
			 -- Time of the last user logout
			 last_logout timestamp DEFAULT 0,
			 -- User online status, if > 0 then user is online, the value
			 -- indicates the number of user connections.
			 -- It is incremented on each user login and decremented on each
			 -- user logout.
			 online_status int default 0,
			 -- Number of failed login attempts
			 failed_logins int default 0,
			 -- User status, whether the account is active or disabled
			 -- >0 - account active, 0 - account disabled
			 account_status int default 1,

			 primary key (uid),
			 unique key sha1_user_id (sha1_user_id),
       key user_id (user_id(765)),
			 key last_login (last_login),
			 key last_logout (last_logout),
			 key account_status (account_status),
			 key online_status (online_status)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table tig_nodes (
       nid bigint unsigned NOT NULL,
       parent_nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       node varchar(255) NOT NULL,

       primary key (nid),
       unique key tnode (parent_nid, uid, node),
       key node (node),
			 constraint tig_nodes_constr foreign key (uid) references tig_users (uid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table tig_pairs (
       nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       pkey varchar(255) NOT NULL,
       pval mediumtext,

       key pkey (pkey),
			 constraint tig_pairs_constr_1 foreign key (uid) references tig_users (uid),
			 constraint tig_pairs_constr_2 foreign key (nid) references tig_nodes (nid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- This is a dummy user who keeps all the database-properties
insert ignore into tig_users (user_id) values ('db-properties');

source database/mysql-schema-4-sp.schema;

select 'Initializing database..';
call TigInitdb();

-- Possible encodings are:
-- - 'MD5-USERID-PASSWORD'
-- - 'MD5-PASSWORD'
-- - 'PLAIN'
-- More can be added if needed.
call TigPutDBProperty('password-encoding', 'PLAIN');
call TigPutDBProperty('schema-version', '4.0');

select 'Adding new user with PlainPw: ', 'test_user', 'test_password';
call TigTestAddUser('test_user', 'test_passwd', 'SUCCESS - adding new user',
		 'ERROR - adding new user');

call TigUserLogin('test_user', 'wrong_passwd', @res_user_id);
select @res_user_id as user_id\g
select if(@res_user_id is NULL,
         'SUCCESS - User login failed as expected, used UserLogin',
			 	 'ERROR - User login succeeded as NOT expected');

call TigUserLoginPlainPw('test_user', 'wrong_passwd', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, used wrong password',
			 	 'ERROR - User login succeeded as NOT expected');

call TigUserLoginPlainPw('test_user', 'test_passwd', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, used UserLoginPlainPw',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');
call TigUserLogout('test_user');
select online_status into @res_online_status from tig_users
  where user_id = 'test_user';
select if(@res_online_status = 0,
			   'SUCCESS - online status OK after 2 logouts',
			 	 'ERROR - online status incorrect after 2 logouts');

select 'Changing password using UpdatePassword';
call TigUpdatePassword('test_user', 'new_password');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, password incorrectly changed',
			 	 'ERROR - User login succeeded as NOT expected');

select 'Changing password using UpdatePasswordPlainPw';
call TigUpdatePasswordPlainPw('test_user', 'new_password');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, password updated with PlainPw',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');
select 'Disabling user account';
call TigDisableAccount('test_user');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is NULL,
			   'SUCCESS - User login failed as expected, account disabled',
			 	 'ERROR - User login succeeded as NOT expected');

select 'Enabling user account';
call TigEnableAccount('test_user');
call TigUserLoginPlainPw('test_user', 'new_password', @res_user_id);
select if(@res_user_id is not NULL,
			   'SUCCESS - User login OK as expected, account enabled',
			 	 'ERROR - User login failed as NOT expected');

call TigUserLogout('test_user');

select 'Adding new user with PlainPw: ', 'test_user_2', 'test_password_2';
call TigTestAddUser('test_user_2', 'test_passwd_2', 'SUCCESS - adding new user',
		 'ERROR - adding new user');

select 'Adding a user with the same user_id: ', 'test_user', 'test_password_2';
call TigTestAddUser('test_user', 'test_password_2', 'ERROR, that was duplicate entry insertion and it should fail.', 'SUCCESS - user adding failure as expected as that was duplicate entry insertion attempt');

call TigRemoveUser('test_user');
call TigRemoveUser('test_user_2');
call TigOnlineUsers();
call TigOfflineUsers();

-- Get top nodes for the user: user1@hostname
--
-- select nid, node from nodes, users
--   where ('user1@hostname' = user_id)
--     AND (nodes.uid = users.uid)
--     AND (parent_nid is null);

-- Get all subnodes of the node: /privacy/default for user: user1@hostname
--
-- select nid, node from nodes,
-- (
--   select nid as dnid from nodes,
--   (
--     select nid as pnid from nodes, users
--       where ('user1@hostname' = user_id)
--         AND (nodes.uid = users.uid)
--         AND (parent_nid is null)
--         AND (node = 'privacy')
--   ) ptab where (parent_nid = pnid)
--       AND (node = 'default')
-- ) dtab where (parent_nid = dnid);

-- Get all keys (pairs) for the node: /privacy/default/24 for user: user1@hostname
--
-- select  pkey, pval from pairs,
-- (
--   select nid, node from nodes,
--   (
--     select nid as dnid from nodes,
--     (
--       select nid as pnid from nodes, users
--         where ('user1@hostname' = user_id)
--           AND (nodes.uid = users.uid)
--     	  AND (parent_nid is null)
--     	  AND (node = 'privacy')
--     ) ptab where (parent_nid = pnid)
--         AND (node = 'default')
--   ) dtab where (parent_nid = dnid)
-- ) ntab where (pairs.nid = ntab.nid) AND (node = '24');
