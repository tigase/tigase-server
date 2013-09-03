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
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $
--

-- QUERY START: USE database
USE [master]
-- QUERY END: USE database
GO

-- QUERY START: create database
CREATE DATABASE ${dbName};
-- QUERY END: create database
GO

-- QUERY START: add user
CREATE LOGIN [${dbUser}] WITH PASSWORD=N'${dbPass}', DEFAULT_DATABASE=[${dbName}]
-- QUERY END: add user
GO

-- QUERY START: ALTER DATABASE
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE is_default=1 AND name = N'PRIMARY') ALTER DATABASE [${dbName}] MODIFY FILEGROUP [PRIMARY] DEFAULT
-- QUERY END: ALTER DATABASE
GO

-- QUERY START: USE DATABASE
USE [${dbName}]
-- QUERY END: USE DATABASE
GO

-- QUERY START: GRANT ALL
CREATE USER [${dbUser}] FOR LOGIN [${dbUser}]
-- QUERY END: GRANT ALL
GO

-- QUERY START: ALTER DATABASE
ALTER USER [${dbUser}] WITH DEFAULT_SCHEMA=[dbo]
-- QUERY END: ALTER DATABASE
GO

-- QUERY START: ALTER DATABASE
ALTER ROLE [db_owner] ADD MEMBER [${dbUser}]
-- QUERY END: ALTER DATABASE
GO
