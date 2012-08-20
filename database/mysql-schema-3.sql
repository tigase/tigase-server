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
       uid bigint unsigned NOT NULL,

       user_id varchar(128) NOT NULL,

       primary key (uid),
       unique key user_id (user_id)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table tig_nodes (
       nid bigint unsigned NOT NULL,
       parent_nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       node varchar(64) NOT NULL,

       primary key (nid),
       unique key tnode (parent_nid, uid, node),
       key node (node),
			 constraint tig_nodes_constr foreign key (uid) references tig_users (uid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

create table tig_pairs (
       nid bigint unsigned,
       uid bigint unsigned NOT NULL,

       pkey varchar(128) NOT NULL,
       pval varchar(65535),

       key pkey (pkey),
			 constraint tig_pairs_constr_1 foreign key (uid) references tig_users (uid),
			 constraint tig_pairs_constr_2 foreign key (nid) references tig_nodes (nid)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- create table tig_max_ids (
--        max_uid bigint unsigned,
--        max_nid bigint unsigned
-- )
-- ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;

-- insert into tig_max_ids (max_uid, max_nid) values (1, 1);

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
