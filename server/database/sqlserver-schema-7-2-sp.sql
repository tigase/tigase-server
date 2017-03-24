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
-- Database stored procedures and functions for Tigase schema version 7.2.0

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- LOAD FILE: database/sqlserver-schema-7-1-sp.sql

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdateLoginTime')
DROP PROCEDURE TigUpdateLoginTime
-- QUERY END:
GO

-- QUERY START:
-- It sets last_login time to the current timestamp
create procedure dbo.TigUpdateLoginTime
	@_user_id nvarchar(2049)
AS
begin
	update dbo.tig_users
		set last_login = CURRENT_TIMESTAMP
		where user_id = @_user_id;
end
-- QUERY END:
GO

-- ------------ Offline Messages
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
	    select @_msg_id = @@IDENTITY;
	end

	select @_msg_id as msg_id;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_GetMessages')
    drop procedure [dbo].[Tig_OfflineMessages_GetMessages];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_GetMessages]
    @_to nvarchar(2049)
as
begin
    select om.message, om.msg_id
        from tig_offline_messages om
        where om.receiver_sha1 = HASHBYTES('SHA1', lower(@_to));
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_GetMessagesByIds')
    drop procedure [dbo].[Tig_OfflineMessages_GetMessagesByIds];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_GetMessagesByIds]
    @_to nvarchar(2049),
    @_msg_id1 nvarchar(50),
    @_msg_id2 nvarchar(50),
    @_msg_id3 nvarchar(50),
    @_msg_id4 nvarchar(50)
as
begin
    select om.message, om.msg_id
    from tig_offline_messages om
    where om.receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
        and (
            (@_msg_id1 is not null and om.msg_id = @_msg_id1)
            or (@_msg_id2 is not null and om.msg_id = @_msg_id2)
            or (@_msg_id3 is not null and om.msg_id = @_msg_id3)
            or (@_msg_id4 is not null and om.msg_id = @_msg_id4)
        );
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_GetMessagesCount')
    drop procedure [dbo].[Tig_OfflineMessages_GetMessagesCount];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_GetMessagesCount]
    @_to nvarchar(2049)
as
begin
    select om.msg_type, count(om.msg_type)
        from tig_offline_messages om
        where om.receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
        group by om.msg_type;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_ListMessages')
    drop procedure [dbo].[Tig_OfflineMessages_ListMessages];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_ListMessages]
    @_to nvarchar(2049)
as
begin
    select om.msg_id, om.msg_type, om.sender
        from tig_offline_messages om
        where om.receiver_sha1 = HASHBYTES('SHA1', lower(@_to));
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
    delete from tig_offline_messages where receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
    select @@ROWCOUNT as affected_rows;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_DeleteMessagesByIds')
    drop procedure [dbo].[Tig_OfflineMessages_DeleteMessagesByIds];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_DeleteMessagesByIds]
    @_to nvarchar(2049),
    @_msg_id1 nvarchar(50),
    @_msg_id2 nvarchar(50),
    @_msg_id3 nvarchar(50),
    @_msg_id4 nvarchar(50)
as
begin
    delete from tig_offline_messages
    where receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
        and (
            (@_msg_id1 is not null and msg_id = @_msg_id1)
            or (@_msg_id2 is not null and msg_id = @_msg_id2)
            or (@_msg_id3 is not null and msg_id = @_msg_id3)
            or (@_msg_id4 is not null and msg_id = @_msg_id4)
        );
    select @@ROWCOUNT as affected_rows;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_DeleteMessage')
    drop procedure [dbo].[Tig_OfflineMessages_DeleteMessage];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_DeleteMessage]
    @_msg_id bigint
as
begin
    delete from tig_offline_messages where msg_id = @_msg_id;
    select @@ROWCOUNT as affected_rows;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_GetExpiredMessages')
    drop procedure [dbo].[Tig_OfflineMessages_GetExpiredMessages];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_GetExpiredMessages]
    @_limit int
as
begin
    select top (@_limit) om.msg_id, om.expired, om.message
        from tig_offline_messages om
        where om.expired is not null
        order by om.expired asc;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_OfflineMessages_GetExpiredMessagesBefore')
    drop procedure [dbo].[Tig_OfflineMessages_GetExpiredMessagesBefore];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_OfflineMessages_GetExpiredMessagesBefore]
    @_expired datetime
as
begin
    select om.msg_id, om.expired, om.message
        from tig_offline_messages om
        where om.expired is not null
            and (@_expired is null or om.expired <= @_expired)
        order by om.expired asc;
end
-- QUERY END:
GO

-- ------------ Broadcast Messages
-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_BroadcastMessages_AddMessage')
    drop procedure [dbo].[Tig_BroadcastMessages_AddMessage];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_BroadcastMessages_AddMessage]
    @_msg_id varchar(128),
    @_expired datetime,
    @_msg nvarchar(max)
as
begin
    if not exists (select 1 from tig_broadcast_messages where id = @_msg_id)
    begin
        insert into tig_broadcast_messages (id, expired, msg)
            values (@_msg_id, @_expired, @_msg);
    end
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_BroadcastMessages_AddMessageRecipient')
    drop procedure [dbo].[Tig_BroadcastMessages_AddMessageRecipient];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_BroadcastMessages_AddMessageRecipient]
    @_msg_id varchar(128),
    @_jid nvarchar(2049)
as
begin
    declare @ErrorMessage nvarchar(max), @ErrorSeverity int, @ErrorState int;
    declare @_jid_sha1 varbinary(20),
            @_jid_id bigint;


    set @_jid_sha1 = HASHBYTES('SHA1', lower(@_jid));

    select @_jid_id = jid_id from tig_broadcast_jids where jid_sha1 = @_jid_sha1;
    if @_jid_id is null
    begin
        begin try
            insert into tig_broadcast_jids (jid, jid_sha1) values (@_jid, @_jid_sha1);
            select @_jid_id = @@IDENTITY;
        end try
        begin catch
            if error_number() = 2627
                select @_jid_id = jid_id from tig_broadcast_jids where jid_sha1 = @_jid_sha1;
            else
            begin
                select @ErrorMessage = ERROR_MESSAGE() + ' Line ' + cast(ERROR_LINE() as nvarchar(5)), @ErrorSeverity = ERROR_SEVERITY(), @ErrorState = ERROR_STATE();
                raiserror (@ErrorMessage, @ErrorSeverity, @ErrorState);
            end
        end catch
    end

    begin try
        insert into tig_broadcast_recipients (msg_id, jid_id)
            select @_msg_id, @_jid_id where not exists (
                select 1 from tig_broadcast_recipients br where br.msg_id = @_msg_id and br.jid_id = @_jid_id
            );
    end try
    begin catch
        if error_number() <> 2627
        begin
            select @ErrorMessage = ERROR_MESSAGE() + ' Line ' + cast(ERROR_LINE() as nvarchar(5)), @ErrorSeverity = ERROR_SEVERITY(), @ErrorState = ERROR_STATE();
            raiserror (@ErrorMessage, @ErrorSeverity, @ErrorState);
        end
    end catch
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_BroadcastMessages_GetMessages')
    drop procedure [dbo].[Tig_BroadcastMessages_GetMessages];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_BroadcastMessages_GetMessages]
    @_expired datetime
as
begin
    select id, expired, msg
    from tig_broadcast_messages
    where expired >= @_expired;
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.objects where type = 'P' and name = 'Tig_BroadcastMessages_GetMessageRecipients')
    drop procedure [dbo].[Tig_BroadcastMessages_GetMessageRecipients];
-- QUERY END:
GO

-- QUERY START:
create procedure [dbo].[Tig_BroadcastMessages_GetMessageRecipients]
    @_msg_id varchar(128)
as
begin
    select j.jid
    from tig_broadcast_recipients r
    inner join tig_broadcast_jids j on j.jid_id = r.jid_id
    where r.msg_id = @_msg_id;
end
-- QUERY END:
GO

