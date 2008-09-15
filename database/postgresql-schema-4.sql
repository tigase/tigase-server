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

--  To load schema to PostgreSQL database execute following commands:
--
--  createuser tigase
--  createdb -U tigase tigase
--  psql -q -U tigase -d tigase -f postgresql-schema.sql


create table tig_users (
       uid serial,

			 -- Jabber User ID
       user_id varchar(2049) NOT NULL,
			 -- User password encrypted or not
			 user_pw varchar(255) default NULL,
			 -- Time of the last user login
			 last_login timestamp,
			 -- Time of the last user logout
			 last_logout timestamp,
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

       primary key (uid)
);
create unique index user_id on tig_users ( user_id );
create index user_pw on tig_users (user_pw);
create index last_login on tig_users (last_login);
create index last_logout on tig_users (last_logout);
create index account_status on tig_users (account_status);
create index online_status on tig_users (online_status);

create table tig_nodes (
       nid serial,
       parent_nid bigint,
       uid bigint NOT NULL references tig_users(uid),

       node varchar(255) NOT NULL,

       primary key (nid)
);
create unique index tnode on tig_nodes ( parent_nid, uid, node );
create index node on tig_nodes ( node );
create index nuid on tig_nodes (uid);
create index parent_nid on tig_nodes (parent_nid);

create table tig_pairs (
       nid bigint references tig_nodes(nid),
       uid bigint NOT NULL references tig_users(uid),

       pkey varchar(255) NOT NULL,
       pval text
);
create index pkey on tig_pairs ( pkey );
create index puid on tig_pairs (uid);
create index pnid on tig_pairs (nid);

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
