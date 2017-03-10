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

-- QUERY START:
create table tig_users (
	uid bigserial,

	-- Jabber User ID
	user_id varchar(2049) NOT NULL,
	-- User password encrypted or not
	user_pw varchar(255) default NULL,
	-- Time the account has been created
	acc_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
-- QUERY END:
-- QUERY START:
create unique index user_id on tig_users ( lower(user_id) );
-- QUERY END:
-- QUERY START:
create index user_pw on tig_users (user_pw);
-- QUERY END:
-- QUERY START:
create index last_login on tig_users (last_login);
-- QUERY END:
-- QUERY START:
create index last_logout on tig_users (last_logout);
-- QUERY END:
-- QUERY START:
create index account_status on tig_users (account_status);
-- QUERY END:
-- QUERY START:
create index online_status on tig_users (online_status);
-- QUERY END:

-- QUERY START:
create table tig_nodes (
       nid bigserial,
       parent_nid bigint,
       uid bigint NOT NULL references tig_users(uid),

       node varchar(255) NOT NULL,

       primary key (nid)
);
-- QUERY END:
-- QUERY START:
create unique index tnode on tig_nodes ( parent_nid, uid, node );
-- QUERY END:
-- QUERY START:
create index node on tig_nodes ( node );
-- QUERY END:
-- QUERY START:
create index nuid on tig_nodes (uid);
-- QUERY END:
-- QUERY START:
create index parent_nid on tig_nodes (parent_nid);
-- QUERY END:

-- QUERY START:
create table tig_pairs (
       nid bigint references tig_nodes(nid),
       uid bigint NOT NULL references tig_users(uid),

       pkey varchar(255) NOT NULL,
       pval text
);
-- QUERY END:
-- QUERY START:
create index pkey on tig_pairs ( pkey );
-- QUERY END:
-- QUERY START:
create index puid on tig_pairs (uid);
-- QUERY END:
-- QUERY START:
create index pnid on tig_pairs (nid);
-- QUERY END:

-- QUERY START:
create table short_news (
  -- Automatic record ID
  snid            bigserial,
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
-- QUERY END:
-- QUERY START:
create index publishing_time on short_news (publishing_time);
-- QUERY END:
-- QUERY START:
create index author on short_news (author);
-- QUERY END:
-- QUERY START:
create index news_type on short_news (news_type);
-- QUERY END:

-- QUERY START:
create table xmpp_stanza (
			 id bigserial,
			 stanza text NOT NULL
);
-- QUERY END:
