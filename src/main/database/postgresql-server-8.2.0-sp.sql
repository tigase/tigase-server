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

-- database properties are deprecated and are being removed

-- QUERY START:
do $$
begin
    if exists( select 1 from pg_proc where proname = lower('TigPutDBProperty')) then
        drop function TigPutDBProperty(varchar(255), text);
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if exists( select 1 from pg_proc where proname = lower('TigUserLogin')) then
        drop function TigUserLogin(varchar(2049), varchar(255));
    end if;
    if exists( select 1 from pg_proc where proname = lower('TigUserLogout')) then
        drop function TigUserLogout(varchar(2049));
    end if;
    if exists( select 1 from pg_proc where proname = lower('TigOnlineUsers')) then
        drop function TigOnlineUsers();
    end if;
    if exists( select 1 from pg_proc where proname = lower('TigOfflineUsers')) then
        drop function TigOfflineUsers();
    end if;
    if exists( select 1 from pg_proc where proname = lower('TigUserLoginPlainPw')) then
        drop function TigUserLoginPlainPw(varchar(2049), varchar(255));
    end if;
end$$;
-- QUERY END:

-- QUERY START:
-- It sets last_login time to the current timestamp
create or replace function TigUpdateLoginTime(varchar(2049)) returns void as $$
declare
    _user_id alias for $1;
begin
    update tig_users
        set last_used = now()
        where lower(user_id) = lower(_user_id);
    return;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
do $$
begin
    if exists( select 1 from pg_proc where proname = lower('Tig_OfflineMessages_DeleteMessage')) then
        drop function Tig_OfflineMessages_DeleteMessage(_msg_id bigint);
    end if;
end$$;
-- QUERY END:

-- QUERY START:
create function Tig_OfflineMessages_DeleteMessage(_msg_id bigint) returns void as $$
begin
    delete from tig_offline_messages where msg_id = _msg_id;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END: