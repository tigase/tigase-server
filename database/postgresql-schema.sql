--  Tigase database schema for PostgreSQL
--
--  Package Tigase XMPP/Jabber Server
--  Copyright (C) 2004 - 2007
--  "Artur Hefczyc" <artur.hefczyc@tigase.org>
--
--  This program is free software; you can redistribute it and/or modify
--  it under the terms of the GNU General Public License as published by
--  the Free Software Foundation; either version 2 of the License, or
--  (at your option) any later version.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
--  GNU General Public License for more details.
--
--  You should have received a copy of the GNU General Public License
--  along with this program; if not, write to the Free Software Foundation,
--  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
--
-- $Rev: 367 $
-- Last modified by $Author: kobit $
-- $Date: 2007-04-03 19:40:48 +0100 (Tue, 03 Apr 2007) $
--

--  To load schema to PostgreSQL database execute following commands:
--
--  createuser tigase
--  createdb -U tigase tigase
--  psql -q -U tigase -d tigase -f postgresql-schema.sql


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
create unique index tnode on tig_nodes ( parent_nid, node );
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
