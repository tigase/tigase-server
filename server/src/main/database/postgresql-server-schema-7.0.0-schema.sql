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
create table if not exists tig_users (
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
do $$
begin
    if to_regclass('public.user_id') is null then
        create unique index user_id on tig_users ( lower(user_id) );
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.user_pw') is null then
        create index user_pw on tig_users (user_pw);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.last_login') is null then
        create index last_login on tig_users (last_login);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.last_logout') is null then
        create index last_logout on tig_users (last_logout);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.account_status') is null then
        create index account_status on tig_users (account_status);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.online_status') is null then
        create index online_status on tig_users (online_status);
    end if;
end$$;
-- QUERY END:

-- QUERY START:
create table if not exists tig_nodes (
       nid bigserial,
       parent_nid bigint,
       uid bigint NOT NULL references tig_users(uid),

       node varchar(255) NOT NULL,

       primary key (nid)
);
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.tnode') is null then
        create unique index tnode on tig_nodes ( parent_nid, uid, node );
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.node') is null then
        create index node on tig_nodes ( node );
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.nuid') is null then
        create index nuid on tig_nodes (uid);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.parent_nid') is null then
        create index parent_nid on tig_nodes (parent_nid);
    end if;
end$$;
-- QUERY END:

-- QUERY START:
create table if not exists tig_pairs (
       nid bigint references tig_nodes(nid),
       uid bigint NOT NULL references tig_users(uid),

       pkey varchar(255) NOT NULL,
       pval text
);
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.pkey') is null then
        create index pkey on tig_pairs ( pkey );
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.puid') is null then
        create index puid on tig_pairs (uid);
    end if;
end$$;
-- QUERY END:
-- QUERY START:
do $$
begin
    if to_regclass('public.pnid') is null then
        create index pnid on tig_pairs (nid);
    end if;
end$$;
-- QUERY END:
