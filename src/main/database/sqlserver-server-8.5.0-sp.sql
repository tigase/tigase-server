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
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_AddMessage')
    drop procedure [dbo].[Tig_OfflineMessages_AddMessage];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_AddMessage]
    @_to nvarchar(2049),
    @_from nvarchar(2049),
    @_type int,
    @_ts datetime,
    @_message nvarchar(max),
    @_expired datetime,
    @_limit bigint
as
begin
    set nocount on;
    declare
        @_msg_count bigint,
        @_msg_id bigint;
    set @_msg_count = 0;

    if @_limit > 0
        select @_msg_count = count(msg_id) from tig_offline_messages where receiver_sha1 = HASHBYTES('SHA1', lower(@_to)) and sender_sha1 = HASHBYTES('SHA1', lower(@_from));

    if @_limit = 0 or @_limit > @_msg_count
    begin
        insert into tig_offline_messages ( receiver, receiver_sha1, sender, sender_sha1, msg_type, ts, message, expired )
            select @_to, HASHBYTES('SHA1', lower(@_to)), @_from, HASHBYTES('SHA1', lower(@_from)), @_type, @_ts, @_message, @_expired;
        set @_msg_id = 1;
    end

    select @_msg_id as msg_id;
    set nocount off;
end
-- QUERY END:
GO

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