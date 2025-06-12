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
drop procedure if exists Tig_OfflineMessages_AddMessage;
-- QUERY END:
-- QUERY START:
drop procedure if exists Tig_OfflineMessages_DeleteMessages;
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

        select 1 as msg_id;
    else
        select null as msg_id;
    end if;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_OfflineMessages_DeleteMessages(_to varchar(2049) charset utf8)
begin
delete from tig_offline_messages where receiver_sha1 = sha1(lower(_to));
end //
-- QUERY END:

delimiter ;