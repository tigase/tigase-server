--
-- Tigase XMPP Server - The instant messaging server
-- Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program. Look for COPYING file in the top folder.
-- If not, see http://www.gnu.org/licenses/.
--

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
    key part_of_user_id (user_id(255)),
	key last_login (last_login),
	key last_logout (last_logout),
	key account_status (account_status),
	key online_status (online_status)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;
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
       pid INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       pkey varchar(255) NOT NULL,
       pval mediumtext character set utf8mb4 collate utf8mb4_unicode_ci,

       key pkey (pkey),
			 key uid (uid),
			 key nid (nid),
			 constraint tig_pairs_constr_1 foreign key (uid) references tig_users (uid),
			 constraint tig_pairs_constr_2 foreign key (nid) references tig_nodes (nid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8;
-- QUERY END:

-- QUERY START:
create table if not exists tig_offline_messages (
    msg_id bigint unsigned not null auto_increment,
    ts timestamp(6) default current_timestamp(6),
    expired timestamp null default null,
    sender varchar(2049),
    sender_sha1 char(128),
    receiver varchar(2049) not null,
    receiver_sha1 char(128) not null,
    msg_type int not null default 0,
    message mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null,
    primary key (msg_id),
    key tig_offline_messages_expired_index (expired),
    key tig_offline_messages_receiver_sha1_index (receiver_sha1),
    key tig_offline_messages_receiver_sha1_sender_sha1_index (receiver_sha1, sender_sha1)
) ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
-- QUERY END:

-- QUERY START:
create table if not exists tig_broadcast_messages (
    id varchar(128) not null,
    expired timestamp(6) not null,
    msg mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null,
    primary key (id)
    );
-- QUERY END:

-- QUERY START:
create table if not exists tig_broadcast_jids (
    jid_id bigint unsigned not null auto_increment,
    jid varchar(2049) not null,
    jid_sha1 char(128) not null,

    primary key (jid_id),
    key tig_broadcast_jids_jid_sha1 (jid_sha1)
);
-- QUERY END:

-- QUERY START:
create table if not exists tig_broadcast_recipients (
    msg_id varchar(128) not null references tig_broadcast_messages(id),
    jid_id bigint not null references tig_broadcast_jids(jid_id),
    primary key (msg_id, jid_id)
);
-- QUERY END:

-- ------------ Clustering support

-- QUERY START:
create table if not exists tig_cluster_nodes (
    hostname varchar(255) not null,
    secondary varchar(512),
    password varchar(255),
    last_update timestamp(6) default current_timestamp(6) on update current_timestamp(6),
    port int,
    cpu_usage double precision unsigned not null,
    mem_usage double precision unsigned not null,
    primary key (hostname)
) ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
-- QUERY END:


-- ------------- Credentials support
-- QUERY START:
create table if not exists tig_user_credentials (
    uid bigint unsigned not null,
    username varchar(2049) not null,
    username_sha1 char(128) not null,
    mechanism varchar(128) not null,
    value mediumtext not null,

    primary key (uid, username_sha1, mechanism),
    constraint tig_credentials_uid foreign key (uid) references tig_users (uid)
) ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
-- QUERY END: