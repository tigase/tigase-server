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

-- Database stored procedures and fucntions for Tigase schema version 5.1


-- QUERY START:
-- It sets last_login time to the current timestamp
create or replace function TigUpdateLoginTime(varchar(2049)) returns void as $$
declare
  _user_id alias for $1;
begin
	update tig_users
		set last_login = now()
		where lower(user_id) = lower(_user_id);
  return;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
do $$
begin
if exists( select 1 from pg_proc where proname = 'tigdisabledaccounts' and pg_get_function_result(oid) = 'TABLE(user_id character varying, last_login timestamp without time zone, last_logout timestamp without time zone, online_status integer, failed_logins integer, account_status integer)') then
    drop function TigDisabledAccounts();
end if;
end$$;
-- QUERY END:

-- QUERY START:
-- Get list of all disabled user accounts
create or replace function TigDisabledAccounts() returns table(user_id varchar(2049), last_login timestamp with time zone, last_logout timestamp with time zone, online_status int, failed_logins int, account_status int) as '
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from tig_users where account_status = 0;
' LANGUAGE 'sql';
-- QUERY END:

-- ------------ Offline Messages
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
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_OfflineMessages_GetExpiredMessages') and pg_get_function_result(oid) = 'TABLE(msg_id bigint, expired timestamp without time zone, message text)') then
    drop function Tig_OfflineMessages_GetExpiredMessages(int);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_OfflineMessages_GetExpiredMessages(_limit int) returns table(
    "msg_id" bigint, "expired" timestamp with time zone, "message" text
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
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_OfflineMessages_GetExpiredMessagesBefore') and pg_get_function_result(oid) = 'TABLE(msg_id bigint, expired timestamp without time zone, message text)') then
    drop function Tig_OfflineMessages_GetExpiredMessagesBefore(timestamp without time zone);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function  Tig_OfflineMessages_GetExpiredMessagesBefore(_expired timestamp with time zone) returns table(
    "msg_id" bigint, "expired" timestamp with time zone, "message" text
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
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_BroadcastMessages_AddMessage') and pg_get_function_arguments(oid) = '_msg_id character varying, _expired timestamp without time zone, _msg text') then
    drop function Tig_BroadcastMessages_AddMessage(_msg_id character varying, _expired timestamp without time zone, _msg text);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_BroadcastMessages_AddMessage(_msg_id varchar(128), _expired timestamp with time zone, _msg text) returns void
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
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_BroadcastMessages_GetMessages') and pg_get_function_arguments(oid) = '_expired timestamp without time zone') then
    drop function Tig_BroadcastMessages_GetMessages(_expired timestamp without time zone);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function  Tig_BroadcastMessages_GetMessages(_expired timestamp with time zone) returns table (
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

-- QUERY START:
CREATE OR REPLACE FUNCTION TigUpdateAccountStatus(_user_id VARCHAR(2049), _status INT)
    RETURNS VOID
AS $$
BEGIN
    UPDATE tig_users
    SET account_status = _status
    WHERE lower(user_id) = lower(_user_id);
    RETURN;
END;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
CREATE OR REPLACE FUNCTION TigAccountStatus(_user_id VARCHAR(2049))
    RETURNS TABLE(status INT)
AS $$
BEGIN
    RETURN QUERY SELECT account_status
                 FROM tig_users
                 WHERE lower(user_id) = lower(_user_id);
END;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- ------------- Credentials support
-- QUERY START:
create or replace function TigUserCredential_Update(_user_id varchar(2049), _username varchar(2049), _mechanism varchar(128), _value text) returns void
as $$
declare _uid bigint;
begin
    select uid into _uid from tig_users where lower(user_id) = lower(_user_id);
    if _uid is not null then
        update tig_user_credentials set value = _value  where uid = _uid and username = _username and mechanism = _mechanism;
        insert into tig_user_credentials (uid, username, mechanism, value)
            select _uid, _username, _mechanism, _value 
            where not exists (select 1 from tig_user_credentials where uid = _uid and username = _username and mechanism = _mechanism);
    end if;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function TigUserCredentials_Get(_user_id varchar(2049), _username varchar(2049)) returns table (
    mechanism varchar(128),
    value text,
    account_status int
) as $$
begin
    return query select c.mechanism, c.value, u.account_status
        from tig_users u
        inner join tig_user_credentials c on c.uid = u.uid
        where
            lower(u.user_id) = lower(_user_id)
            and c.username = _username;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function TigUserUsernames_Get(_user_id varchar(2049)) returns table (
    username varchar(2049)
) as $$
begin
    return query select distinct c.username
        from tig_users u
        inner join tig_user_credentials c on c.uid = u.uid
        where
            lower(u.user_id) = lower(_user_id);
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function TigUserCredential_Remove(_user_id varchar(2049), _username varchar(2049)) returns void
as $$
declare _uid bigint;
begin
    select uid into _uid from tig_users where lower(user_id) = lower(_user_id);
    if _uid is not null then
        delete from tig_user_credentials where uid = _uid and username = _username;
    end if;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
-- _user_id character varying, _user_pw character varying
-- Add a new user to the database assuming the user password is already
-- encoded properly according to the database settings.
-- If password is not encoded TigAddUserPlainPw should be used instead.
create or replace function TigAddUserPlainPw(_user_id varchar(2049), _user_pw varchar(255))
  returns bigint as $$
declare
  _res_uid bigint;
begin
	insert into tig_users (user_id)
		values (_user_id);
	select currval('tig_users_uid_seq') into _res_uid;

	insert into tig_nodes (parent_nid, uid, node)
		values (NULL, _res_uid, 'root');

    if _user_pw is null then
        update tig_users set account_status = -1 where lower(user_id) = lower(_user_id);
    else
        perform TigUpdatePasswordPlainPw(_user_id, _user_pw);
    end if;

	return _res_uid as uid;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
do $$
begin
if exists( select 1 from pg_proc where proname = lower('TigGetPassword') and pg_get_function_result(oid) = 'character varying') then
    drop function TigGetPassword(character varying);
end if;
end$$;
-- QUERY END:

-- QUERY START:
-- Returns user's password from the database
create or replace function TigGetPassword(_user_id varchar(2049)) returns text as $$
declare
  res_pw text;
begin
    select c.value into res_pw
    from tig_users u
    inner join tig_user_credentials c on c.uid = u.uid
    where
        lower(u.user_id) = lower(_user_id)
        and c.username = 'default'
        and c.mechanism = 'PLAIN';
    return res_pw;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
create or replace function TigUpdatePasswordPlainPw(_user_id varchar(2049), _user_pw varchar(255))
  returns void as $$
declare
    _passwordEncoding text;
    _encodedPassword text;
begin
	select coalesce(TigGetDBProperty('password-encoding'), 'PLAIN') into _passwordEncoding;
	
    select
        case _passwordEncoding
		    when 'MD5-PASSWORD' then MD5(_user_pw)
		    when 'MD5-USERID-PASSWORD' then MD5(_user_id || _user_pw)
	        when 'MD5-USERNAME-PASSWORD' then MD5(split_part(_user_id, '@', 1) || _user_pw)
		    else _user_pw
		end into _encodedPassword;

	perform TigUserCredential_Update(_user_id, 'default', _passwordEncoding, _encodedPassword);
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create or replace function TigUserLoginPlainPw(_user_id varchar(2049), _user_pw varchar(255))
returns varchar(2049) as $$
declare
    _res_user_id varchar(2049);
    _passwordEncoding text;
    _encodedPassword text;
begin
	select coalesce(TigGetDBProperty('password-encoding'), 'PLAIN') into _passwordEncoding;

    select
        case _passwordEncoding
		    when 'MD5-PASSWORD' then MD5(_user_pw)
		    when 'MD5-USERID-PASSWORD' then MD5(_user_id || _user_pw)
	        when 'MD5-USERNAME-PASSWORD' then MD5(split_part(_user_id, '@', 1) || _user_pw)
		    else _user_pw
		end into _encodedPassword;

	select u.user_id into _res_user_id
	from tig_users u
	inner join tig_user_credentials c on c.uid = u.uid
	where
	    lower(u.user_id) = lower(_user_id)
	    and c.username = 'default'
	    and c.mechanism = _passwordEncoding
	    and c.value = _encodedPassword
	    and u.account_status > 0;

    if _res_user_id is not null then
		update tig_users
			set online_status = online_status + 1, last_login = now()
			where lower(user_id) = lower(_user_id);
	else
		update tig_users set failed_logins = failed_logins + 1 where lower(user_id) = lower(_user_id);
    end if;

    return _res_user_id;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Removes a user from the database
create or replace function TigRemoveUser(_user_id varchar(2049)) returns void as $$
declare
  res_uid bigint;
begin
	select uid into res_uid from tig_users where lower(user_id) = lower(_user_id);

    delete from tig_user_credentials where uid = res_uid;
	delete from tig_pairs where uid = res_uid;
	delete from tig_nodes where uid = res_uid;
	delete from tig_users where uid = res_uid;
end;
$$ language 'plpgsql';
-- QUERY END:

-- QUERY START:
-- Database properties set - procedure
create or replace function TigPutDBProperty(varchar(255), text) returns void as '
declare
  _tkey alias for $1;
  _tval alias for $2;
begin
  if exists( select pval from tig_pairs, tig_users where
		(lower(user_id) = lower(''db-properties'')) AND (tig_users.uid = tig_pairs.uid)
		AND (pkey = _tkey))
  then
	  update tig_pairs set pval = _tval from tig_users
      where (lower(tig_users.user_id) = lower(''db-properties''))
        AND (tig_users.uid = tig_pairs.uid)
        AND (pkey = _tkey);
  else
    if not exists( select 1 from tig_users where user_id = ''db-properties'' ) then
      perform TigAddUserPlainPw(''db-properties'', NULL);
    end if;
    insert into tig_pairs (pkey, pval, uid, nid)
		  select _tkey, _tval, tu.uid, tn.nid from tig_users tu  left join tig_nodes tn on tn.uid=tu.uid
			  where (lower(user_id) = lower(''db-properties'')  and tn.node=''root'' );
  end if;
  return;
end;
' LANGUAGE 'plpgsql';
-- QUERY END: