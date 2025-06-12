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
drop procedure if exists TigServerUpgrade;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if not exists (select 1 from information_schema.STATISTICS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_offline_messages' and INDEX_NAME = 'tig_offline_messages_receiver_sha1_msg_type_index') then
        create index tig_offline_messages_receiver_sha1_msg_type_index on tig_offline_messages (receiver_sha1, msg_type);
    end if;
    if exists (select 1 from information_schema.STATISTICS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_offline_messages' and INDEX_NAME = 'tig_offline_messages_receiver_sha1_index') then
        drop index tig_offline_messages_receiver_sha1_index on tig_offline_messages;
    end if;
end //
-- QUERY END:

delimiter ;

-- QUERY START:
call TigServerUpgrade();
-- QUERY END:

-- QUERY START:
drop procedure if exists TigServerUpgrade;
-- QUERY END:


