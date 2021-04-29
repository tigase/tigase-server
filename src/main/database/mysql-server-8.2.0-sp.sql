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
drop procedure if exists TigPutDBProperty;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigUserLogin;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLogout;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigOnlineUsers;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigOfflineUsers;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigAllUsers;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUserLoginPlainPw;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigUpdateLoginTime;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_OfflineMessages_DeleteMessage;
-- QUERY END:


delimiter //

-- QUERY START:
-- List of all users in database
create procedure TigAllUsers()
begin
    select user_id, failed_logins, account_status
    from tig_users;
end //
-- QUERY END:

-- QUERY START:
-- It sets last_used time to the current timestamp
create procedure TigUpdateLoginTime(_user_id varchar(2049) charset utf8mb4 )
begin
    update tig_users
    set last_used = CURRENT_TIMESTAMP
    where sha1_user_id = sha1(lower(_user_id));
end //
-- QUERY END:

-- QUERY START:
    create procedure Tig_OfflineMessages_DeleteMessage(_msg_id bigint)
    begin
        delete from tig_offline_messages where msg_id = _msg_id;
    end //
-- QUERY END:

delimiter ;
