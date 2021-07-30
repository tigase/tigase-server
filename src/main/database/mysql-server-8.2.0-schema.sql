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
    if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_users' and column_name = 'online_status') then
        alter table tig_users
            drop column online_status,
            drop column last_logout,
            change column last_login last_used timestamp null default null;
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

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if exists(SELECT 1 FROM information_schema.statistics s1 WHERE s1.table_schema = database() AND s1.table_name = 'tig_users' AND s1.index_name = 'online_status') then
        drop index online_status on tig_users;
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

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if exists(SELECT 1 FROM information_schema.statistics s1 WHERE s1.table_schema = database() AND s1.table_name = 'tig_users' AND s1.index_name = 'last_login') then
        drop index last_login on tig_users;
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

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if exists(SELECT 1 FROM information_schema.statistics s1 WHERE s1.table_schema = database() AND s1.table_name = 'tig_users' AND s1.index_name = 'last_logout') then
        drop index last_logout on tig_users;
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

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if exists(SELECT 1 FROM information_schema.statistics s1 WHERE s1.table_schema = database() AND s1.table_name = 'tig_offline_messages' AND s1.index_name = 'tig_offline_messages_receiver_sha1_index') then
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

delimiter //

-- QUERY START:
create procedure TigServerUpgrade()
begin
    if exists(select * from information_schema.COLUMNS where TABLE_SCHEMA = database() and TABLE_NAME = 'tig_offline_messages' and COLUMN_NAME = 'sender_sha1' and CHARACTER_MAXIMUM_LENGTH = 128) and exists(select * from information_schema.COLUMNS where TABLE_SCHEMA = database() and TABLE_NAME = 'tig_offline_messages' and COLUMN_NAME = 'receiver_sha1' and CHARACTER_MAXIMUM_LENGTH = 128) then
        alter table tig_offline_messages modify column receiver_sha1 char(40) not null, modify column sender_sha1 char(40);
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