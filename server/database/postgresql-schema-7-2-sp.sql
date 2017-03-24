--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

-- Database stored procedures and fucntions for Tigase schema version 5.1

\i database/postgresql-schema-7-1-sp.sql

-- LOAD FILE: database/postgresql-schema-7-1-sp.sql


-- QUERY START:
-- It sets last_login time to the current timestamp
create or replace function TigUpdateLoginTime(varchar(2049)) returns void as '
declare
  _user_id alias for $1;
begin
	update tig_users
		set last_login = now()
		where lower(user_id) = lower(_user_id);
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END:


-- ------------ Offline Messages
-- QUERY START:
create or replace function Tig_OfflineMessages_AddMessage(_to varchar(2049), _from varchar(2049), _type int, _ts timestamp, _message text, _expired timestamp, _limit bigint) returns bigint as $$
declare
    _msg_count bigint;
    _msg_id bigint;
begin
    perform _msg_count = 0;

    if _limit > 0  then
        select count(msg_id) into _msg_count from tig_offline_messages where lower(receiver) = lower(_to) and lower(sender) = lower(_from);
    end if;

    if _limit = 0 or _limit > _msg_count then
        with inserted_msg as (
	        insert into tig_offline_messages ( receiver, sender, msg_type, ts, message, expired )
	            values ( _to, _from, _type, _ts, _message, _expired )
	        returning msg_id
        )
	    select msg_id into _msg_id from inserted_msg;
	end if;

	return _msg_id;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function  Tig_OfflineMessages_GetMessages(_to varchar(2049)) returns table(
    "message" text, "msg_id" bigint
) as $$
begin
    return query select om.message, om.msg_id
        from tig_offline_messages om
        where lower(om.receiver) = lower(_to);
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function  Tig_OfflineMessages_GetMessagesByIds(_to varchar(2049), _msg_id1 varchar(50), _msg_id2 varchar(50), _msg_id3 varchar(50), _msg_id4 varchar(50)) returns table(
    "message" text, "msg_id" bigint
) as $$
begin
    return query select om.message, om.msg_id
        from tig_offline_messages om
        where lower(om.receiver) = lower(_to)
            and (
                (_msg_id1 is not null and om.msg_id = cast(_msg_id1 as bigint))
                or (_msg_id2 is not null and om.msg_id = cast(_msg_id2 as bigint))
                or (_msg_id3 is not null and om.msg_id = cast(_msg_id3 as bigint))
                or (_msg_id4 is not null and om.msg_id = cast(_msg_id4 as bigint))
            );
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_GetMessagesCount(_to varchar(2049)) returns table(
    "msg_type" int, "count" bigint
) as $$
begin
    return query select om.msg_type, count(om.msg_type)
        from tig_offline_messages om
        where lower(om.receiver) = lower(_to)
        group by om.msg_type;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function  Tig_OfflineMessages_ListMessages(_to varchar(2049)) returns table(
    "msg_id" bigint, "msg_type" int, "sender" varchar(2049)
) as $$
begin
    return query select om.msg_id, om.msg_type, om.sender
        from tig_offline_messages om
        where lower(om.receiver) = lower(_to);
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_DeleteMessages(_to varchar(2049)) returns bigint as $$
declare _deleted bigint;
begin
    with deleted as (
        delete from tig_offline_messages where lower(receiver) = lower(_to)
        returning msg_id
    )
    select count(msg_id) into _deleted from deleted;
    return _deleted;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_DeleteMessagesByIds(_to varchar(2049), _msg_id1 varchar(50), _msg_id2 varchar(50), _msg_id3 varchar(50), _msg_id4 varchar(50)) returns bigint as $$
declare _deleted bigint;
begin
    with deleted as (
        delete from tig_offline_messages
        where lower(receiver) = lower(_to)
            and (
                (_msg_id1 is not null and msg_id = cast(_msg_id1 as bigint))
                or (_msg_id2 is not null and msg_id = cast(_msg_id2 as bigint))
                or (_msg_id3 is not null and msg_id = cast(_msg_id3 as bigint))
                or (_msg_id4 is not null and msg_id = cast(_msg_id4 as bigint))
            )
        returning msg_id
    )
    select count(msg_id) into _deleted from deleted;
    return _deleted;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_DeleteMessage(_msg_id bigint) returns bigint as $$
declare _deleted bigint;
begin
    with deleted as (
        delete from tig_offline_messages where msg_id = _msg_id
        returning msg_id
    )
    select count(msg_id) into _deleted from deleted;
    return _deleted;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_GetExpiredMessages(_limit int) returns table(
    "msg_id" bigint, "expired" timestamp, "message" text
) as $$
begin
    return query select om.msg_id, om.expired, om.message
        from tig_offline_messages om
        where om.expired is not null
        order by om.expired asc
        limit _limit;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function  Tig_OfflineMessages_GetExpiredMessagesBefore(_expired timestamp) returns table(
    "msg_id" bigint, "expired" timestamp, "message" text
) as $$
begin
    return query select om.msg_id, om.expired, om.message
        from tig_offline_messages om
        where om.expired is not null
            and (_expired is null or om.expired <= _expired)
        order by om.expired asc;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:


-- ------------ Broadcast Messages
-- QUERY START:
create or replace function Tig_BroadcastMessages_AddMessage(_msg_id varchar(128), _expired timestamp, _msg text) returns void
as $$
begin
    begin
        insert into tig_broadcast_messages (id, expired, msg)
            select _msg_id, _expired, _msg
            where not exists (
                select 1 from tig_broadcast_messages where id = _msg_id
            );
    exception when unique_violation then
    end;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_BroadcastMessages_AddMessageRecipient(_msg_id varchar(128), _jid varchar(2049)) returns void
as $$
declare _jid_id bigint;
begin
    select jid_id into _jid_id from tig_broadcast_jids where lower(jid) = lower(_jid);
    if _jid_id is null then
        begin
            with inserted as (
                insert into tig_broadcast_jids (jid)
                    values (_jid)
                returning jid_id
            )
            select jid_id into _jid_id from inserted;
        exception when unique_violation then
            select jid_id into _jid_id from tig_broadcast_jids where lower(jid) = lower(_jid);
        end;
    end if;

    begin
        insert into tig_broadcast_recipients (msg_id, jid_id)
            select _msg_id, _jid_id where not exists (
                select 1 from tig_broadcast_recipients br where br.msg_id = _msg_id and br.jid_id = _jid_id
            );
    exception when unique_violation then
    end;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function  Tig_BroadcastMessages_GetMessages(_expired timestamp) returns table (
    id varchar(128),
    expired timestamp,
    msg text
) as $$
begin
    return query select bm.id, bm.expired, bm.msg
    from tig_broadcast_messages bm
    where bm.expired >= _expired;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_BroadcastMessages_GetMessageRecipients(_msg_id varchar(128)) returns table (
    jid varchar(2049)
) as $$
begin
    return query select j.jid
    from tig_broadcast_recipients r
    inner join tig_broadcast_jids j on j.jid_id = r.jid_id
    where r.msg_id = _msg_id;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

