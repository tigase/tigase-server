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
    if exists( select 1 from pg_proc where proname = lower('Tig_OfflineMessages_AddMessage') and pg_get_function_arguments(oid) = '_to character varying, _from character varying, _type integer, _ts timestamp without time zone, _message text, _expired timestamp without time zone, _limit bigint') then
        drop function Tig_OfflineMessages_AddMessage(_to character varying, _from character varying, _type integer, _ts timestamp without time zone, _message text, _expired timestamp without time zone, _limit bigint);
    end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_AddMessage(_to varchar(2049), _from varchar(2049), _type int, _ts timestamp with time zone, _message text, _expired timestamp with time zone, _limit bigint) returns bigint as $$
declare
    _msg_count bigint;
    _msg_id bigint;
begin
    perform _msg_count = 0;

    if _limit > 0  then
        select count(msg_id) into _msg_count from tig_offline_messages where lower(receiver) = lower(_to) and lower(sender) = lower(_from);
    end if;

    if _limit = 0 or _limit > _msg_count then
	    insert into tig_offline_messages ( receiver, sender, msg_type, ts, message, expired )
	        values ( _to, _from, _type, _ts, _message, _expired )
        select 1 into _msg_id from inserted_msg;
    end if;

    return _msg_id;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
do $$
begin
    if exists( select 1 from pg_proc where proname = lower('Tig_OfflineMessages_DeleteMessages')) then
drop function Tig_OfflineMessages_DeleteMessages(varchar(2049));
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_DeleteMessages(_to varchar(2049)) returns void as $$
begin
    delete from tig_offline_messages where lower(receiver) = lower(_to)
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END: