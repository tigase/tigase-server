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
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

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
		set last_login = GETUTCDATE()
		where user_id = @_user_id;
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLogin')
DROP PROCEDURE TigUserLogin
-- QUERY END:
GO

-- QUERY START:
-- Perforrm user login. It returns user_id uppon success and NULL
-- on failure.
-- If the login is successful it also increases online_status and sets
-- last_login time to the current timestamp
create procedure dbo.TigUserLogin
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	if exists(select 1 from dbo.tig_users
		where (account_status > 0) AND (sha1_user_id = hashbytes('SHA1', lower(@_user_id)))
			AND (user_pw = @_user_pw) AND (user_id = @_user_id))
		begin
		update dbo.tig_users
			set online_status = online_status + 1, last_login = GETUTCDATE()
				where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
			select @_user_id as user_id;
		end
	else
		begin
			update dbo.tig_users set failed_logins = failed_logins + 1 where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
			select NULL as user_id;
		end
	end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLogout')
DROP PROCEDURE TigUserLogout
-- QUERY END:
GO

-- QUERY START:
-- It decreases online_status and sets last_logout time to the current timestamp
create procedure dbo.TigUserLogout
	@_user_id nvarchar(2049)
AS
begin
	update dbo.tig_users
		set online_status = dbo.InlineMax(online_status - 1, 0),
			last_logout = GETUTCDATE()
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
	    select @_msg_id = @@IDENTITY;
	end

	select @_msg_id as msg_id;
	set nocount off;
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
    set nocount on;
    delete from tig_offline_messages where receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
    select @@ROWCOUNT as affected_rows;
    set nocount off;
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
    set nocount on;
    delete from tig_offline_messages
    where receiver_sha1 = HASHBYTES('SHA1', lower(@_to))
        and (
            (@_msg_id1 is not null and msg_id = @_msg_id1)
            or (@_msg_id2 is not null and msg_id = @_msg_id2)
            or (@_msg_id3 is not null and msg_id = @_msg_id3)
            or (@_msg_id4 is not null and msg_id = @_msg_id4)
        );
    select @@ROWCOUNT as affected_rows;
    set nocount off;
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
    set nocount on;
    delete from tig_offline_messages where msg_id = @_msg_id;
    select @@ROWCOUNT as affected_rows;
    set nocount off;
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

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdateAccountStatus')
DROP PROCEDURE [dbo].[TigUpdateAccountStatus]
-- QUERY END:
GO

-- QUERY START:
-- Upate account status
CREATE PROCEDURE [dbo].[TigUpdateAccountStatus]
        @_user_id NVARCHAR(2049),
        @_status  INT
AS
    BEGIN
        UPDATE dbo.tig_users
        SET account_status = @_status
        WHERE user_id = @_user_id;
    END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAccountStatus')
    DROP PROCEDURE [dbo].[TigAccountStatus]
-- QUERY END:
GO

-- QUERY START:
-- Returns account_status
CREATE PROCEDURE [dbo].[TigAccountStatus]
        @_user_id NVARCHAR(2049)
AS
    BEGIN
        SELECT account_status
        FROM tig_users
        WHERE user_id = @_user_id;
    END
-- QUERY END:
GO

-- ------------- Credentials support
-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserCredential_Update')
DROP PROCEDURE TigUserCredential_Update
-- QUERY END:
GO

-- QUERY START:
-- It sets last_login time to the current timestamp
create procedure dbo.TigUserCredential_Update
	@_user_id nvarchar(2049),
	@_username nvarchar(2049),
	@_mechanism nvarchar(128),
	@_value nvarchar(max)
AS
begin
    declare @_uid bigint;
    declare @_username_sha1 varbinary(32);

    select @_uid = uid, @_username_sha1 = HASHBYTES('SHA1', @_username) from tig_users where sha1_user_id = HASHBYTES('SHA1', lower(@_user_id));
    if @_uid is not null
    begin
        update tig_user_credentials set value = @_value where uid = @_uid and username_sha1 = @_username_sha1 and mechanism = @_mechanism;
        if @@ROWCOUNT = 0
        begin
            begin try
                insert into tig_user_credentials (uid, username, username_sha1, mechanism, value)
                    select @_uid, @_username, @_username_sha1, @_mechanism, @_value
                    where not exists (
                        select 1 from tig_user_credentials
                        where
                            uid = @_uid
                            and username_sha1 = @_username_sha1
                            and mechanism = @_mechanism
                    );
            end try
            begin catch
            	IF ERROR_NUMBER() <> 2627
                declare @ErrorMessage nvarchar(max), @ErrorSeverity int, @ErrorState int;
                select @ErrorMessage = ERROR_MESSAGE() + ' Line ' + cast(ERROR_LINE() as nvarchar(5)), @ErrorSeverity = ERROR_SEVERITY(), @ErrorState = ERROR_STATE();
				raiserror (@ErrorMessage, @ErrorSeverity, @ErrorState);
            end catch
        end
    end
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserCredentials_Get')
DROP PROCEDURE TigUserCredentials_Get
-- QUERY END:
GO

-- QUERY START:
-- It sets last_login time to the current timestamp
create procedure dbo.TigUserCredentials_Get
	@_user_id nvarchar(2049),
	@_username nvarchar(2049)
AS
begin
    select c.mechanism, c.value, u.account_status
    from tig_users u
    inner join tig_user_credentials c on c.uid = u.uid
    where
        u.sha1_user_id = HASHBYTES('SHA1', lower(@_user_id))
        and c.username_sha1 = HASHBYTES('SHA1', @_username);
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserUsernames_Get')
DROP PROCEDURE TigUserUsernames_Get
-- QUERY END:
GO

-- QUERY START:
create procedure dbo.TigUserUsernames_Get
	@_user_id nvarchar(2049)
AS
begin
    select distinct
        c.username
    from tig_users u
    inner join tig_user_credentials c on c.uid = u.uid
    where
        u.sha1_user_id = HASHBYTES('SHA1', lower(@_user_id));
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserCredential_Remove')
DROP PROCEDURE TigUserCredential_Remove
-- QUERY END:
GO

-- QUERY START:
-- It sets last_login time to the current timestamp
create procedure dbo.TigUserCredential_Remove
	@_user_id nvarchar(2049),
	@_username nvarchar(2049)
AS
begin
    declare @_uid bigint;
    declare @_username_sha1 varbinary(32);

    select @_uid = uid, @_username_sha1 = HASHBYTES('SHA1', @_username) from tig_users where sha1_user_id = HASHBYTES('SHA1', lower(@_user_id));

    delete from tig_user_credentials
    where
        uid = @_uid
        and username_sha1 = @_username_sha1;
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAddUserPlainPw')
DROP PROCEDURE TigAddUserPlainPw
-- QUERY END:
GO

-- QUERY START:
-- Add a new user to the database assuming the user password is already
-- encoded properly according to the database settings.
-- If password is not encoded TigAddUserPlainPw should be used instead.
create procedure dbo.TigAddUserPlainPw
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
	begin
		SET NOCOUNT ON;

		declare @res_uid bigint;

		insert into dbo.tig_users (user_id, sha1_user_id)
			values (@_user_id, HASHBYTES('SHA1', LOWER(@_user_id)));

		set  @res_uid = (select SCOPE_IDENTITY());

		if (@res_uid is not NULL)
			insert into dbo.tig_nodes (parent_nid, uid, node)
				values (NULL, @res_uid, N'root');

		if (@_user_pw is NULL)
			update dbo.tig_users set account_status = -1 where uid = @res_uid;
		else
		    exec TigUpdatePasswordPlainPw @_user_id, @_user_pw

		select @res_uid as uid;
	end;
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigGetPassword')
DROP PROCEDURE TigGetPassword
-- QUERY END:
GO

-- QUERY START:
-- Returns user's password from the database
create procedure dbo.TigGetPassword
	@_user_id nvarchar(2049)
AS
begin
    select c.value
    from tig_users u
    inner join tig_user_credentials c on c.uid = u.uid
    where
        u.sha1_user_id = hashbytes('SHA1', lower(@_user_id))
        and c.username_sha1 = hashbytes('SHA1', N'default')
        and c.mechanism = N'PLAIN';
end
-- QUERY END:
GO


-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdatePasswordPlainPw')
DROP PROCEDURE TigUpdatePasswordPlainPw
-- QUERY END:
GO

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
create procedure dbo.TigUpdatePasswordPlainPw
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	declare @_encoding nvarchar(512)
	declare @_encoded nvarchar(max)
	set @_encoding = coalesce(dbo.TigGetDBProperty(N'password-encoding'), N'PLAIN')

	set @_encoded = case @_encoding
	    when N'MD5-PASSWORD' then HASHBYTES('MD5', @_user_pw)
	    when N'MD5-USERID-PASSWORD' then HASHBYTES('MD5', @_user_id + @_user_pw)
	    when N'MD5-USERNAME-PASSWORD' then HASHBYTES('MD5', (LEFT (@_user_id, CHARINDEX(N'@',@_user_id)-1)) + @_user_pw)
	    else @_user_pw
	    end;

	exec TigUserCredential_Update @_user_id, N'default', @_encoding, @_encoded
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLoginPlainPw')
DROP PROCEDURE TigUserLoginPlainPw
-- QUERY END:
GO

-- QUERY START:
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create procedure dbo.TigUserLoginPlainPw
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	declare @_encoding nvarchar(512)
	declare @_encoded nvarchar(max)
	set @_encoding = coalesce(dbo.TigGetDBProperty(N'password-encoding'), N'PLAIN')

	set @_encoded = case @_encoding
	    when N'MD5-PASSWORD' then HASHBYTES('MD5', @_user_pw)
	    when N'MD5-USERID-PASSWORD' then HASHBYTES('MD5', @_user_id + @_user_pw)
	    when N'MD5-USERNAME-PASSWORD' then HASHBYTES('MD5', (LEFT (@_user_id, CHARINDEX(N'@',@_user_id)-1)) + @_user_pw)
	    else @_user_pw
	    end;

    if exists (select 1 from dbo.tig_users u
        inner join tig_user_credentials c on c.uid = u.uid
		where u.sha1_user_id = hashbytes('SHA1', lower(@_user_id))
		    and u.account_status > 0
		    and c.username_sha1 = hashbytes('SHA1', N'default')
		    and c.mechanism = @_encoding
			and c.value = @_encoded)
		begin
		update dbo.tig_users
			set online_status = online_status + 1, last_login = CURRENT_TIMESTAMP
				where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
			select @_user_id as user_id;
		end
	else
		begin
			update dbo.tig_users set failed_logins = failed_logins + 1 where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
			select NULL as user_id;
		end
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigRemoveUser')
DROP PROCEDURE TigRemoveUser
-- QUERY END:
GO

-- QUERY START:
-- Removes a user from the database
create procedure dbo.TigRemoveUser
	@_user_id nvarchar(2049)
AS
begin
	declare @res_uid bigint;

	set @res_uid = (select uid from dbo.tig_users where sha1_user_id = hashbytes('SHA1', lower(@_user_id)));

    delete from dbo.tig_user_credentials where uid = @res_uid;
	delete from dbo.tig_pairs where uid = @res_uid;
	delete from dbo.tig_nodes where uid = @res_uid;
	delete from dbo.tig_users where uid = @res_uid;
end
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigPutDBProperty')
DROP PROCEDURE TigPutDBProperty
-- QUERY END:
GO

-- QUERY START:
-- Database properties set - procedure
create procedure dbo.TigPutDBProperty
	@_tkey nvarchar(255),
	@_tval ntext
	AS
	begin
		Declare @_nid int;
		Declare @_uid int;
		Declare @_count int;
		if exists (select 1 from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey))
			begin
				select @_nid = dbo.tig_pairs.nid, @_uid = dbo.tig_pairs.uid from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey);
				update dbo.tig_pairs set pval = @_tval
					where (@_uid = uid) AND (pkey = @_tkey) ;
			end
		else
			begin
			    if not exists (select 1 from tig_users where user_id = 'db-properties')
                    exec dbo.TigAddUserPlainPw 'db-properties', NULL;


				select @_nid = dbo.tig_pairs.nid, @_uid = dbo.tig_pairs.uid from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey);
				insert into dbo.tig_pairs (pkey, pval, uid, nid)
					select @_tkey, @_tval, tu.uid, tn.nid from dbo.tig_users tu  left join tig_nodes tn on tn.uid=tu.uid
						where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')) and tn.node='root');

			end
	end
-- QUERY END:
GO
