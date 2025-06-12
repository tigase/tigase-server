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
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_DeleteMessages')
    drop procedure [dbo].[Tig_OfflineMessages_DeleteMessages];
-- QUERY END:
GO

-- QUERY START:
create procedure  [dbo].[Tig_OfflineMessages_DeleteMessages]
    @_to nvarchar(2049)
as
begin
    set nocount on;
    delete from tig_offline_messages where receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
    set nocount off;
end
-- QUERY END:
GO