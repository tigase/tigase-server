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
IF EXISTS (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_last_login' )
    DROP INDEX [IX_tig_users_last_login] ON [dbo].[tig_users]
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_last_logout' )
    DROP INDEX [IX_tig_users_last_logout] ON [dbo].[tig_users]
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_online_status' )
    DROP INDEX [IX_tig_users_online_status] ON [dbo].[tig_users]
-- QUERY END:
GO

-- QUERY START:
IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'last_used' AND object_id = object_id('dbo.tig_users'))
BEGIN
    ALTER TABLE [tig_users] ADD [last_used] [datetime] default 0;
END
-- QUERY END:
GO
-- QUERY START:
IF EXISTS (SELECT * FROM sys.columns WHERE name = 'last_login' AND object_id = object_id('dbo.tig_users'))
BEGIN
    UPDATE [tig_users] SET [last_used] = [last_login] WHERE [last_used] = 0;
END
-- QUERY END:
GO
-- QUERY START:
IF EXISTS (SELECT * FROM sys.columns WHERE name = 'online_status' AND object_id = object_id('dbo.tig_users'))
BEGIN
    DECLARE @sqlStmt nvarchar(MAX)
    DECLARE C CURSOR LOCAL STATIC READ_ONLY FORWARD_ONLY FOR
        select 'ALTER TABLE tig_users DROP CONSTRAINT ' + dc.name + ';'
        from sys.default_constraints as dc
            left join sys.columns as sc on dc.parent_column_id = sc.column_id
        where dc.parent_object_id = OBJECT_ID('dbo.tig_users')
            and type_desc = 'DEFAULT_CONSTRAINT'
            and sc.name in ('online_status', 'last_login', 'last_logout')
    OPEN C
    FETCH NEXT FROM C INTO @sqlStmt
    WHILE @@FETCH_STATUS = 0
    BEGIN
        exec sp_executesql @sqlStmt
        fetch next from c into @sqlStmt
    END
    ALTER TABLE [tig_users] DROP COLUMN IF EXISTS [online_status], [last_logout], [last_login]
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_offline_messages_receiver_sha1' )
    DROP INDEX [IX_tig_offline_messages_receiver_sha1] ON [dbo].[tig_offline_messages]
-- QUERY END:
GO