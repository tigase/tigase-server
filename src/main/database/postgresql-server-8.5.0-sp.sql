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