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
-- Database stored procedures and functions for Tigase schema version 7.1.0

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