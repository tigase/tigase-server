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

--  To load schema to PostgreSQL database execute following commands:
--
--  createuser tigase
--  createdb -U tigase tigase
--  psql -q -U tigase -d tigase -f postgresql-schema.sql


create table short_news (
  -- Automatic record ID
  snid            serial,
  -- Automaticly generated timestamp and automaticly updated on change
  publishing_time timestamp default now(),
	-- Optional news type: 'shorts', 'minis', 'techs', 'funs'....
	news_type				varchar(10),
  -- Author JID
  author          varchar(128) NOT NULL,
  -- Short subject - this is short news, right?
  subject         varchar(128) NOT NULL,
  -- Short news message - this is short news, right?
  body            varchar(1024) NOT NULL,
  primary key(snid)
);
create index publishing_time on short_news (publishing_time);
create index author on short_news (author);
create index news_type on short_news (news_type);

create table xmpp_stanza (
			 id serial,
			 stanza text NOT NULL
);

create table tig_users (
       uid bigint NOT NULL,

       user_id varchar(128) NOT NULL,

       primary key (uid)
);
create unique index user_id on tig_users ( user_id );

create table tig_nodes (
       nid bigint NOT NULL,
       parent_nid bigint,
       uid bigint NOT NULL references tig_users(uid),

       node varchar(64) NOT NULL,

       primary key (nid)
);
create unique index tnode on tig_nodes ( parent_nid, uid, node );
create index node on tig_nodes ( node );

create table tig_pairs (
       nid bigint references tig_nodes(nid),
       uid bigint NOT NULL references tig_users(uid),

       pkey varchar(128) NOT NULL,
       pval varchar(65535)
);
create index pkey on tig_pairs ( pkey );

create table tig_max_ids (
       max_uid bigint,
       max_nid bigint
);


insert into tig_max_ids (max_uid, max_nid) values (1, 1);

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
