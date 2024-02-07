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
drop procedure if exists Tig_BroadcastMessages_AddMessage;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_BroadcastMessages_AddMessageRecipient;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserCredential_Update;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserCredential_Remove;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure Tig_BroadcastMessages_AddMessage(_msg_id varchar(128), _expired timestamp, _msg mediumtext charset utf8mb4)
begin
    -- DO NOT REMOVE, required for properly handle exceptions within transactions!
    DECLARE exit handler for sqlexception
    BEGIN
        -- ERROR
        ROLLBACK;
        RESIGNAL;
    END;

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
    -- DO NOT REMOVE, required for properly handle exceptions within transactions!
    DECLARE exit handler for sqlexception
    BEGIN
        -- ERROR
        ROLLBACK;
        RESIGNAL;
    END;

    start transaction;
        select jid_id into _jid_id from tig_broadcast_jids where jid_sha1 = sha1(lower(_jid));
        if _jid_id is null then
            insert into tig_broadcast_jids (jid, jid_sha1)
                values (_jid, sha1(lower(_jid)))
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
create procedure TigUserCredential_Update(_user_id varchar(2049) CHARSET utf8, _username varchar(2049) CHARSET utf8, _mechanism varchar(128) CHARSET utf8, _value mediumtext CHARSET utf8)
begin
    declare _uid bigint;
    declare _user_id_sha1 char(128);
    declare _username_sha1 char(128);
    -- DO NOT REMOVE, required for properly handle exceptions within transactions!
    DECLARE exit handler for sqlexception
    BEGIN
        -- ERROR
        ROLLBACK;
        RESIGNAL;
    END;

    select uid into _uid from tig_users where sha1_user_id = sha1(lower(_user_id));

    if _uid is not null then
        start transaction;
            insert into tig_user_credentials (uid, username, username_sha1, mechanism, value)
                values (_uid, _username, sha1(_username), _mechanism, _value)
                on duplicate key update value = _value;
        commit;
    end if;
end //
-- QUERY END:

-- QUERY START:
create procedure TigUserCredential_Remove(_user_id varchar(2049) CHARSET utf8, _username varchar(2049) CHARSET utf8)
begin
    declare _uid bigint;
    declare _user_id_sha1 char(128);
    -- DO NOT REMOVE, required for properly handle exceptions within transactions!
    DECLARE exit handler for sqlexception
    BEGIN
        -- ERROR
        ROLLBACK;
        RESIGNAL;
    END;

    select uid into _uid from tig_users where sha1_user_id = sha1(lower(_user_id));

    if _uid is not null then
        start transaction;
            delete from tig_user_credentials where uid = _uid and username_sha1 = sha1(_username);
        commit;
    end if;
end //
-- QUERY END: