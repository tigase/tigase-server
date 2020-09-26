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

-- database properties are deprecated and are being removed

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigPutDBProperty')
    DROP PROCEDURE TigPutDBProperty
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
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigUserLoginPlainPw')
    DROP PROCEDURE TigUserLoginPlainPw
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigAllUsers')
    DROP PROCEDURE TigAllUsers
-- QUERY END:
GO

-- QUERY START:
-- List of all users in database
create procedure dbo.TigAllUsers
AS
begin
    select user_id, failed_logins, account_status from dbo.tig_users;
end
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
        set last_used = GETUTCDATE()
        where user_id = @_user_id;
end
-- QUERY END:
GO