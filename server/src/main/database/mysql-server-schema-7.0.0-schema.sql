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

--  To load schema to MySQL database execute following commands:
--
--  mysqladmin -u root -pdbpass create tigase
--  mysql -u root -pdbpass tigase < database/mysql-schema-4.sql
--  echo "GRANT ALL ON tigase.* TO tigase_user@'%' \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql
--  echo "GRANT ALL ON tigase.* TO tigase_user@'localhost' \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql
--  echo "GRANT ALL ON tigase.* TO tigase_user \
--                  IDENTIFIED BY 'tigase_passwd'; \
--                  FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql

-- QUERY START:

create table if not exists tig_users (
	uid bigint unsigned NOT NULL auto_increment,

	-- Jabber User ID
	user_id varchar(2049) NOT NULL,
	-- UserID SHA1 hash to prevent duplicate user_ids
	sha1_user_id char(128) NOT NULL,
	-- User password encrypted or not
	user_pw varchar(255) default NULL,
	-- Time the account has been created
	acc_create_time timestamp DEFAULT CURRENT_TIMESTAMP,
	-- Time of the last user login
	last_login timestamp NULL DEFAULT NULL,
	-- Time of the last user logout
	last_logout timestamp NULL DEFAULT NULL,
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
	key user_pw (user_pw),
--	key user_id (user_id(765)),
	key last_login (last_login),
	key last_logout (last_logout),
	key account_status (account_status),
	key online_status (online_status)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

-- QUERY END:

-- QUERY START:
drop procedure if exists TigExecuteIf;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigExecuteIfNot;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigExecuteIf(cond int, query text)
begin
set @s = (select if (
        cond > 0,
        query,
        'select 1'
    ));
prepare stmt from @s;
execute stmt;
deallocate prepare stmt;
end //
-- QUERY END:

-- QUERY START:
create procedure TigExecuteIfNot(cond int, query text)
begin
set @s = (select if (
        cond <= 0,
        query,
        'select 1'
    ));
prepare stmt from @s;
execute stmt;
deallocate prepare stmt;
end //
-- QUERY END:

delimiter ;

-- QUERY START:
call TigExecuteIf(
	(select count(1) from information_schema.columns where table_schema = database() and table_name = 'tig_users' and IS_NULLABLE = 'NO' and COLUMN_DEFAULT = '0000-00-00 00:00:00'),
	"alter table tig_users modify column last_login timestamp NULL DEFAULT NULL, modify column last_logout timestamp NULL DEFAULT NULL"
);
-- QUERY END:

-- QUERY START:
call TigExecuteIfNot(
    (select count(1) from information_schema.statistics where table_schema = database() and table_name = 'tig_users' and index_name = 'part_of_user_id'),
    "CREATE INDEX part_of_user_id ON tig_users (user_id(255))"
);
-- QUERY END:

-- QUERY START:

create table if not exists tig_nodes (
       nid bigint unsigned NOT NULL auto_increment,
       parent_nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       node varchar(255) NOT NULL,

       primary key (nid),
       unique key tnode (parent_nid, uid, node),
       key node (node),
			 key uid (uid),
			 key parent_nid (parent_nid),
			 constraint tig_nodes_constr foreign key (uid) references tig_users (uid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- QUERY END:

-- QUERY START:

create table if not exists tig_pairs (
       nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       pkey varchar(255) NOT NULL,
       pval mediumtext,

       key pkey (pkey),
			 key uid (uid),
			 key nid (nid),
			 constraint tig_pairs_constr_1 foreign key (uid) references tig_users (uid),
			 constraint tig_pairs_constr_2 foreign key (nid) references tig_nodes (nid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;

-- QUERY END:
