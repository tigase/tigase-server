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
do $$
begin
    if exists (select 1 where (select to_regclass('public.last_login')) is not null) then
        drop index last_login;
    end if;
    if exists (select 1 where (select to_regclass('public.last_logout')) is not null) then
        drop index last_logout;
    end if;
    if exists (select 1 where (select to_regclass('public.online_status')) is not null) then
        drop index online_status;
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_users' and column_name = 'last_login') then
        alter table tig_users rename column last_login to last_used;
    end if;
    if exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_users' and column_name = 'last_logout') then
        alter table tig_users drop column last_logout;
    end if;
    if exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_users' and column_name = 'online_status') then
        alter table tig_users drop column online_status;
    end if;
end$$;
-- QUERY END: