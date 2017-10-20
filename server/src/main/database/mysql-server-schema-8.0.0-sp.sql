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
--

-- Database stored procedures and functions for Tigase schema version 5.1

source database/mysql-server-schema-7.1.0-sp.sql;

-- LOAD FILE: database/mysql-server-schema-7.1.0-sp.sql

-- QUERY START:
drop procedure if exists TigUpdateLoginTime;
-- QUERY END:

delimiter //

-- QUERY START:
-- It sets last_login time to the current timestamp
create procedure TigUpdateLoginTime(_user_id varchar(2049) charset utf8mb4 )
begin
	update tig_users
		set last_login = CURRENT_TIMESTAMP
		where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

delimiter ;

-- QUERY START:
drop procedure if exists TigUpdatePairs;
-- QUERY END:

delimiter //

-- QUERY START:
-- Procedure to efficiently and safely update data in tig_pairs table
create procedure TigUpdatePairs(_nid bigint, _uid bigint, _tkey varchar(255) CHARSET utf8, _tval mediumtext CHARSET utf8mb4)
begin
  if exists(SELECT 1 FROM tig_pairs WHERE nid = _nid AND uid = _uid AND pkey = _tkey)
  then
    UPDATE tig_pairs SET pval = _tval WHERE nid = _nid AND uid = _uid AND pkey = _tkey;
  ELSE
    INSERT INTO tig_pairs (nid, uid, pkey, pval) VALUES (_nid, _uid, _tkey, _tval);
  END IF;
end //
-- QUERY END:


delimiter ;

-- QUERY START:
drop procedure if exists Tig_OfflineMessages_AddMessage;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_GetMessages;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_GetMessagesByIds;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_GetMessagesCount;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_ListMessages;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_DeleteMessages;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_DeleteMessagesByIds;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_DeleteMessage;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_GetExpiredMessages;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_GetExpiredMessagesBefore;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure Tig_OfflineMessages_AddMessage(_to varchar(2049) charset utf8, _from varchar(2049) charset utf8, _type int, _ts timestamp(6), _message mediumtext charset utf8mb4, _expired timestamp(6), _limit bigint)
begin
    declare msg_count bigint;
    set msg_count = 0;

    if _limit > 0  then
        select count(msg_id) into msg_count from tig_offline_messages where receiver_sha1 = sha1(lower(_to)) and sender_sha1 = sha1(lower(_from));
    end if;

    if _limit = 0 or _limit > msg_count then
	    insert into tig_offline_messages ( receiver, receiver_sha1, sender, sender_sha1, msg_type, ts, message, expired )
	        values ( _to, sha1(lower(_to)), _from, sha1(lower(_from)), _type, _ts, _message, _expired );

	    select last_insert_id() as msg_id;
	else
	    select null as msg_id;
	end if;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_GetMessages(_to varchar(2049) charset utf8)
begin
    select message, msg_id
    from tig_offline_messages
    where receiver_sha1 = sha1(lower(_to));
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_GetMessagesByIds(_to varchar(2049) charset utf8, _msg_id1 varchar(50) charset utf8, _msg_id2 varchar(50) charset utf8, _msg_id3  varchar(50) charset utf8, _msg_id4 varchar(50) charset utf8)
begin
    select message, msg_id
    from tig_offline_messages
    where receiver_sha1 = sha1(lower(_to))
        and (
            (_msg_id1 is not null and msg_id = _msg_id1)
            or (_msg_id2 is not null and msg_id = _msg_id2)
            or (_msg_id3 is not null and msg_id = _msg_id3)
            or (_msg_id4 is not null and msg_id = _msg_id4)
        );
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_GetMessagesCount(_to varchar(2049) charset utf8)
begin
    select msg_type , count(msg_type)
    from tig_offline_messages
    where receiver_sha1 = sha1(lower(_to))
    group by msg_type;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_ListMessages(_to varchar(2049) charset utf8)
begin
    select msg_id, msg_type, sender
    from tig_offline_messages
    where receiver_sha1 = sha1(lower(_to));
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_DeleteMessages(_to varchar(2049) charset utf8)
begin
    delete from tig_offline_messages where receiver_sha1 = sha1(lower(_to));
    select row_count() as deleted_rows;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_DeleteMessagesByIds(_to varchar(2049) charset utf8, _msg_id1 varchar(50) charset utf8, _msg_id2 varchar(50) charset utf8, _msg_id3  varchar(50) charset utf8, _msg_id4 varchar(50) charset utf8)
begin
    delete from tig_offline_messages
    where receiver_sha1 = sha1(lower(_to))
        and (
            (_msg_id1 is not null and msg_id = _msg_id1)
            or (_msg_id2 is not null and msg_id = _msg_id2)
            or (_msg_id3 is not null and msg_id = _msg_id3)
            or (_msg_id4 is not null and msg_id = _msg_id4)
        );
    select row_count() as deleted_rows;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_DeleteMessage(_msg_id bigint)
begin
    delete from tig_offline_messages where msg_id = _msg_id;
    select row_count() as deleted_rows;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_GetExpiredMessages(_limit int)
begin
    select msg_id, expired, message
    from tig_offline_messages
    where expired is not null
    order by expired asc
    limit _limit;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_GetExpiredMessagesBefore(_expired timestamp(6))
begin
    select msg_id, expired, message
    from tig_offline_messages
    where expired is not null
        and (_expired is null or expired <= _expired)
    order by expired asc;
end //
-- QUERY END:

delimiter ;

-- ------------

-- QUERY START:
drop procedure if exists TigUpdateAccountStatus;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAccountStatus;
-- QUERY END:

delimiter //

-- QUERY START:
-- Set user account status
create procedure TigUpdateAccountStatus(_user_id varchar(2049) CHARSET utf8, _status INT)
    begin
        update tig_users set account_status = _status where sha1_user_id = sha1(lower(_user_id));
    end //
-- QUERY END:

-- QUERY START:
-- Get user account status
create procedure TigAccountStatus(_user_id varchar(2049) CHARSET utf8)
    begin
        select account_status from tig_users where sha1_user_id = sha1(lower(_user_id));
    end //
-- QUERY END:

delimiter ;

-- ------------ Broadcast Messages

-- QUERY START:
drop procedure if exists Tig_BroadcastMessages_AddMessage;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_BroadcastMessages_AddMessageRecipient;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_BroadcastMessages_GetMessages;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_BroadcastMessages_GetMessageRecipients;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure Tig_BroadcastMessages_AddMessage(_msg_id varchar(128), _expired timestamp, _msg mediumtext charset utf8mb4)
begin
    start transaction;
        insert into tig_broadcast_messages (id, expired, msg)
            values (_msg_id, _expired, _msg)
            on duplicate key update expired = expired;
    commit;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_BroadcastMessages_AddMessageRecipient(_msg_id varchar(128), _jid varchar(2049))
begin
    declare _jid_id bigint;
    declare _jid_sha1 char(128);

    select sha1(lower(_jid)) into _jid_sha1;

    start transaction;
        select jid_id into _jid_id from tig_broadcast_jids where jid_sha1 = _jid_sha1;
        if _jid_id is null then
            insert into tig_broadcast_jids (jid, jid_sha1)
                values (_jid, _jid_sha1)
                on duplicate key update jid_id = LAST_INSERTED_ID(jid_id);
            select LAST_INSERTED_ID() into _jid_id;
        end if;

        insert into tig_broadcast_recipients (msg_id, jid_id)
            values (_msg_id, _jid_id)
            on duplicate key update jid_id = jid_id;
    commit;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_BroadcastMessages_GetMessages(_expired timestamp(6))
begin
    select id, expired, msg
    from tig_broadcast_messages
    where expired >= _expired;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_BroadcastMessages_GetMessageRecipients(_msg_id varchar(128))
begin
    select j.jid
    from tig_broadcast_recipients r
    inner join tig_broadcast_jids j on j.jid_id = r.jid_id
    where r.msg_id = _msg_id;
end //
-- QUERY END:

delimiter ;

-- ------------- Credentials support

-- QUERY START:
drop procedure if exists TigUserCredential_Update;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserCredentials_Get;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserCredential_Remove;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLoginPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAddUserPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigGetPassword;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdatePasswordPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigRemoveUser;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigUserCredential_Update(_user_id varchar(2049) CHARSET utf8, _username varchar(2049) CHARSET utf8, _mechanism varchar(128) CHARSET utf8, _value mediumtext CHARSET utf8)
begin
    declare _uid bigint;
    declare _user_id_sha1 char(128);
    declare _username_sha1 char(128);

    select sha1(lower(_user_id)), sha1(_username) into _user_id_sha1, _username_sha1;

    select uid into _uid from tig_users where sha1_user_id = _user_id_sha1;

    if _uid is not null then
        start transaction;
            insert into tig_user_credentials (uid, username, username_sha1, mechanism, value)
                values (_uid, _username, _username_sha1, _mechanism, _value)
                on duplicate key update value = _value;
        commit;
    end if;
end //
-- QUERY END:

-- QUERY START:
create procedure TigUserCredentials_Get(_user_id varchar(2049) CHARSET utf8, _username varchar(2049) CHARSET utf8)
begin
    declare _user_id_sha1 char(128);
    declare _username_sha1 char(128);

    select sha1(lower(_user_id)), sha1(_username) into _user_id_sha1, _username_sha1;

    select mechanism, value, account_status
    from tig_users u
    inner join tig_user_credentials c on u.uid = c.uid
    where
        u.sha1_user_id = _user_id_sha1
        and c.username_sha1 = _username_sha1;
end //
-- QUERY END:

-- QUERY START:
create procedure TigUserCredential_Remove(_user_id varchar(2049) CHARSET utf8, _username varchar(2049) CHARSET utf8)
begin
    declare _uid bigint;
    declare _user_id_sha1 char(128);
    declare _username_sha1 char(128);

    select sha1(lower(_user_id)), sha1(_username) into _user_id_sha1, _username_sha1;

    select uid into _uid from tig_users where sha1_user_id = _user_id_sha1;

    if _uid is not null then
        start transaction;
            delete from tig_user_credentials where uid = _uid and username_sha1 = _username_sha1;
        commit;
    end if;
end //
-- QUERY END:

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
-- and creates a new user account.
create procedure TigAddUserPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
	declare res_uid bigint unsigned;

	insert into tig_users (user_id, sha1_user_id)
		values (_user_id, sha1(lower(_user_id)));

	select LAST_INSERT_ID() into res_uid;

	insert into tig_nodes (parent_nid, uid, node)
		values (NULL, res_uid, 'root');

	if _user_pw is NULL then
		update tig_users set account_status = -1 where uid = res_uid;
    else
        call TigUpdatePasswordPlainPw(_user_id, _user_pw);
   	end if;

	select res_uid as uid;
end //
-- QUERY END:

-- QUERY START:
-- Returns user's password from the database
create procedure TigGetPassword(_user_id varchar(2049) CHARSET utf8)
begin
	select c.value
	from tig_users u
	inner join tig_user_credentials c on c.uid = u.uid
	where
	    u.sha1_user_id = sha1(lower(_user_id))
	    and c.mechanism = 'PLAIN'
	    and c.username_sha1 = sha1('default');
end //
-- QUERY END:

-- QUERY START:
create procedure TigUpdatePasswordPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
    declare _passwordEncoding varchar(128) CHARSET utf8;
    declare _encodedPassword varchar(255) CHARSET utf8;

    select IFNULL(TigGetDBProperty('password-encoding'), 'PLAIN') into _passwordEncoding;
    select case _passwordEncoding
		when 'MD5-PASSWORD' then
			MD5(_user_pw)
		when 'MD5-USERID-PASSWORD' then
			MD5(CONCAT(_user_id, _user_pw))
		when 'MD5-USERNAME-PASSWORD' then
			MD5(CONCAT(substring_index(_user_id, '@', 1), _user_pw))
		else
			_user_pw
		end into _encodedPassword;

    call TigUserCredential_Update(_user_id, 'default', _passwordEncoding, _encodedPassword);
end //
-- QUERY END:

-- QUERY START:
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create procedure TigUserLoginPlainPw(_user_id varchar(2049) CHARSET utf8, _user_pw varchar(255) CHARSET utf8)
begin
    declare _passwordEncoding varchar(128) CHARSET utf8;
    declare _encodedPassword varchar(255) CHARSET utf8;

    select IFNULL(TigGetDBProperty('password-encoding'), 'PLAIN') into _passwordEncoding;
    select case _passwordEncoding
		when 'MD5-PASSWORD' then
			MD5(_user_pw)
		when 'MD5-USERID-PASSWORD' then
			MD5(CONCAT(_user_id, _user_pw))
		when 'MD5-USERNAME-PASSWORD' then
			MD5(CONCAT(substring_index(_user_id, '@', 1), _user_pw))
		else
			_user_pw
		end into _encodedPassword;

	if exists(select 1 from tig_users u
	    inner join tig_user_credentials c on c.uid = u.uid
	    where (u.account_status > 0) AND (u.sha1_user_id = sha1(lower(_user_id))) AND (u.user_id = _user_id)
	        AND (c.username_sha1 = sha1('default'))
	        AND (c.mechanism = _passwordEncoding) AND (c.value = _encodedPassword))
	then
		update tig_users
			set online_status = online_status + 1, last_login = CURRENT_TIMESTAMP
			where sha1_user_id = sha1(lower(_user_id));
		select _user_id as user_id;
	else
		update tig_users set failed_logins = failed_logins + 1 where sha1_user_id = sha1(lower(_user_id));
		select NULL as user_id;
    end if;
end //
-- QUERY END:


-- Removes a user from the database
-- QUERY START:
create procedure TigRemoveUser(_user_id varchar(2049) CHARSET utf8)
begin
	declare res_uid bigint unsigned;

	select uid into res_uid from tig_users where sha1_user_id = sha1(lower(_user_id));

    delete from tig_user_credentials where uid = res_uid;
	delete from tig_pairs where uid = res_uid;
	delete from tig_nodes where uid = res_uid;
	delete from tig_users where uid = res_uid;
end //
-- QUERY END:

delimiter ;