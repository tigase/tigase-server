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
drop procedure if exists TigExecuteIf;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigExecuteIfNot;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigExecuteIf(cond int, query text)
begin
set @s = (select if (
        cond > 0,
        query,
        'select 1'
    ));
prepare stmt from @s;
execute stmt;
deallocate prepare stmt;
end //
-- QUERY END:

-- QUERY START:
create procedure TigExecuteIfNot(cond int, query text)
begin
set @s = (select if (
        cond <= 0,
        query,
        'select 1'
    ));
prepare stmt from @s;
execute stmt;
deallocate prepare stmt;
end //
-- QUERY END:

delimiter ;