--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
-- Database stored procedures and functions for Tigase schema version 5.2.0

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigInitdb')
DROP PROCEDURE TigInitdb
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAddUserPlainPw')
DROP PROCEDURE TigAddUserPlainPw
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAddUser')
DROP PROCEDURE TigAddUser
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigGetUserDBUid')
DROP PROCEDURE TigGetUserDBUid
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigRemoveUser')
DROP PROCEDURE TigRemoveUser
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdatePasswordPlainPw')
DROP PROCEDURE TigUpdatePasswordPlainPw
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdatePassword')
DROP PROCEDURE TigUpdatePassword
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdatePasswordPlainPwRev')
DROP PROCEDURE TigUpdatePasswordPlainPwRev
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigGetPassword')
DROP PROCEDURE TigGetPassword
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLoginPlainPw')
DROP PROCEDURE TigUserLoginPlainPw
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLogin')
DROP PROCEDURE TigUserLogin
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLogout')
DROP PROCEDURE TigUserLogout
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigOnlineUsers')
DROP PROCEDURE TigOnlineUsers
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigOfflineUsers')
DROP PROCEDURE TigOfflineUsers
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAllUsers')
DROP PROCEDURE TigAllUsers
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAllUsersCount')
DROP PROCEDURE TigAllUsersCount
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigDisableAccount')
DROP PROCEDURE TigDisableAccount
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigEnableAccount')
DROP PROCEDURE TigEnableAccount
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigActiveAccounts')
DROP PROCEDURE TigActiveAccounts
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigDisabledAccounts')
DROP PROCEDURE TigDisabledAccounts
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAddNode')
DROP PROCEDURE TigAddNode
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigTestAddUser')
DROP PROCEDURE TigTestAddUser
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUsers2Ver4Convert')
DROP PROCEDURE TigUsers2Ver4Convert
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigPutDBProperty')
DROP PROCEDURE TigPutDBProperty
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'FN' AND name = 'TigGetDBProperty')
DROP FUNCTION TigGetDBProperty
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUpdatePairs')
DROP PROCEDURE TigUpdatePairs
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'FN' AND name = 'InlineMax')
DROP FUNCTION InlineMax
-- QUERY END:
GO




-- QUERY START:
-- The initialization of the database.
-- The procedure should be called manually somehow before starting the
-- server. In theory the server could call the procedure automatically
-- at the startup time but I don't know yet how to solve the problem
-- with multiple cluster nodes starting at later time when the server
-- is already running.
create procedure dbo.TigInitdb
AS
begin
  update dbo.tig_users set online_status = 0;
end
-- QUERY END:
GO

-- QUERY START:
-- Database properties get - function
create function TigGetDBProperty(@_tkey nvarchar(255)) returns nvarchar(MAX)
AS
begin
--Declare @_result nvarchar(MAX);
return (select pval  from dbo.tig_pairs AS p, dbo.tig_users AS u
		where (pkey = @_tkey) AND (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
					AND (p.uid = u.uid));

end
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
				select @_nid = dbo.tig_pairs.nid, @_uid = dbo.tig_pairs.uid from dbo.tig_pairs, dbo.tig_users
					where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')))
						AND (dbo.tig_users.uid = dbo.tig_pairs.uid)  AND (pkey = @_tkey);
				insert into dbo.tig_pairs (pkey, pval, uid)
					select @_tkey, @_tval, uid from dbo.tig_users
						where (sha1_user_id = HASHBYTES('SHA1', LOWER(N'db-properties')));
			end
	end
-- QUERY END:
GO


-- QUERY START:
-- Add a new user to the database assuming the user password is already
-- encoded properly according to the database settings.
-- If password is not encoded TigAddUserPlainPw should be used instead.
create procedure dbo.TigAddUser
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
	begin
		SET NOCOUNT ON;
		
		declare @res_uid bigint;

		insert into dbo.tig_users (user_id, sha1_user_id, user_pw)
			values (@_user_id, HASHBYTES('SHA1', LOWER(@_user_id)) , @_user_pw);

		set  @res_uid = (select SCOPE_IDENTITY());

		if (@res_uid is not NULL)
			insert into dbo.tig_nodes (parent_nid, uid, node)
				values (NULL, @res_uid, N'root');

		if (@_user_pw is NULL) 
			update dbo.tig_users set account_status = -1 where uid = @res_uid;

		select @res_uid as uid;
	end;
-- QUERY END:
GO

-- QUERY START:
-- Takes plain text user password and converts it to internal representation
-- and creates a new user account.
create procedure dbo.TigAddUserPlainPw
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	declare @_encoding nvarchar(512)
	declare @_hashed_pass varbinary(32)
	set @_encoding = dbo.TigGetDBProperty(N'password-encoding')
	if @_encoding = N'MD5-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_pw);
			exec TigAddUser @_user_id, @_hashed_pass;
		end
	if @_encoding = N'MD5-USERID-PASSWORD' 
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_id + @_user_pw);
			exec TigAddUser @_user_id, @_hashed_pass
		end
	if @_encoding = N'MD5-USERNAME-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', (LEFT (@_user_id, CHARINDEX(N'@',@_user_id)-1)) + @_user_pw);
			exec TigAddUser @_user_id, @_hashed_pass;
		end
	else
		exec TigAddUser @_user_id, @_user_pw ;
	end
-- QUERY END:
GO

-- QUERY START:
-- Low level database user id as big number. Used only for performance reasons
-- and save database space. Besides JID is too large to server as UID
create procedure dbo.TigGetUserDBUid
	@_user_id nvarchar(2049)
AS	
begin
	select uid from dbo.tig_users where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
end
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

	delete from dbo.tig_pairs where uid = @res_uid;
	delete from dbo.tig_nodes where uid = @res_uid;
	delete from dbo.tig_users where uid = @res_uid;
end
-- QUERY END:
GO

-- QUERY START:
-- Returns user's password from the database
create procedure dbo.TigGetPassword
	@_user_id nvarchar(2049)
AS
begin
	select user_pw from dbo.tig_users where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
end
-- QUERY END:
GO

-- QUERY START:
-- Update user password
create procedure dbo.TigUpdatePassword
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	update dbo.tig_users set user_pw = @_user_pw where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
end
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
	declare @_hashed_pass varbinary(32)
	set @_encoding = dbo.TigGetDBProperty(N'password-encoding')
	if @_encoding = N'MD5-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_pw);
			exec TigUpdatePassword @_user_id, @_hashed_pass;
		end
	if @_encoding = N'MD5-USERID-PASSWORD' 
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_id + @_user_pw);
			exec TigUpdatePassword @_user_id, @_hashed_pass
		end
	if @_encoding = N'MD5-USERNAME-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', (LEFT (@_user_id, CHARINDEX(N'@',@_user_id)-1)) + @_user_pw);
			exec TigUpdatePassword @_user_id, @_hashed_pass;
		end
	else
		exec TigUpdatePassword @_user_id, @_user_pw ;
end
-- QUERY END:
GO

-- QUERY START:
-- Variant of TigUpdatePasswordPlainPw SP with parameters in reverse order.
-- Some implementations require the parameters to be in the same order as
-- the update query.
create procedure dbo.TigUpdatePasswordPlainPwRev
	@_user_pw nvarchar(255),
	@_user_id nvarchar(2049)
AS
begin
	exec TigUpdatePasswordPlainPw @_user_id, @_user_pw;
end
-- QUERY END:
GO

-- QUERY START:
-- List all online users
create procedure dbo.TigOnlineUsers
AS
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from dbo.tig_users where online_status > 0;
end
-- QUERY END:
GO

-- QUERY START:
-- List all offline users
create procedure dbo.TigOfflineUsers
AS
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from dbo.tig_users where online_status = 0;
end
-- QUERY END:
GO

-- QUERY START:
-- List of all users in database
create procedure dbo.TigAllUsers
AS
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from dbo.tig_users;
end
-- QUERY END:
GO

-- QUERY START:
-- All users count
create procedure dbo.TigAllUsersCount
AS
begin
	select count(*) from dbo.tig_users;
end
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
-- Performs user login for a plain text password, converting it to an internal
-- representation if necessary
create procedure dbo.TigUserLoginPlainPw
	@_user_id nvarchar(2049),
	@_user_pw nvarchar(255)
AS
begin
	declare @_encoding nvarchar(512)
	declare @_hashed_pass varbinary(32)
	set @_encoding = dbo.TigGetDBProperty(N'password-encoding')
	if @_encoding = 'MD5-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_pw);
			exec TigUserLogin @_user_id, @_hashed_pass;
		end
	if @_encoding = N'MD5-USERID-PASSWORD' 
		begin
			set @_hashed_pass = HASHBYTES('MD5', @_user_id + @_user_pw);
			exec TigUserLogin @_user_id, @_hashed_pass
		end
	if @_encoding = N'MD5-USERNAME-PASSWORD'
		begin
			set @_hashed_pass = HASHBYTES('MD5', (LEFT (@_user_id, CHARINDEX(N'@',@_user_id)-1)) + @_user_pw);
			exec TigUserLogin @_user_id, @_hashed_pass;
		end
	else
		exec TigUserLogin @_user_id, @_user_pw ;
	end
-- QUERY END:
GO

-- QUERY START:
-- helper function
create function dbo.InlineMax(@val1 int, @val2 int)
returns int
as
begin
  if @val1 > @val2
    return @val1
  return isnull(@val2,@val1)
end
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
			last_logout = CURRENT_TIMESTAMP
		where user_id = @_user_id;
end
-- QUERY END:
GO

-- QUERY START:
-- Disable user account
create procedure dbo.TigDisableAccount
	@_user_id nvarchar(2049)
AS
begin
	update dbo.tig_users set account_status = 0 where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
end
-- QUERY END:
GO

-- QUERY START:
-- Enable user account
create procedure dbo.TigEnableAccount
	@_user_id nvarchar(2049)
AS
begin
	update dbo.tig_users set account_status = 1 where sha1_user_id = hashbytes('SHA1', lower(@_user_id));
end
-- QUERY END:
GO

-- QUERY START:
-- Get list of all active user accounts
create procedure dbo.TigActiveAccounts
AS
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from dbo.tig_users where account_status > 0;
end
-- QUERY END:
GO

-- QUERY START:
-- Get list of all disabled user accounts
create procedure dbo.TigDisabledAccounts
AS
begin
	select user_id, last_login, last_logout, online_status, failed_logins, account_status
		from dbo.tig_users where account_status = 0;
end
-- QUERY END:
GO

-- QUERY START:
-- Helper procedure for adding a new node
create procedure dbo.TigAddNode
@_parent_nid bigint,
@_uid bigint,
@_node nvarchar(255)
AS
begin
	SET NOCOUNT ON;
	
	insert into dbo.tig_nodes (parent_nid, uid, node)
		values (@_parent_nid, @_uid, @_node);
	select SCOPE_IDENTITY() as nid;
end
-- QUERY END:
GO

-- QUERY START:
-- Procedure to efficiently and safely update data in tig_pairs table
create procedure dbo.TigUpdatePairs
@_nid bigint,
@_uid bigint,
@_tkey nvarchar(255),
@_tval ntext
AS
begin
  if exists(SELECT 1 FROM dbo.tig_pairs WHERE nid = @_nid AND uid = @_uid AND pkey = @_tkey)
    UPDATE dbo.tig_pairs SET pval = @_tval WHERE nid = @_nid AND uid = @_uid AND pkey = @_tkey;
  ELSE
    INSERT INTO dbo.tig_pairs (nid, uid, pkey, pval) VALUES (@_nid, @_uid, @_tkey, @_tval);
END
-- QUERY END:
GO


