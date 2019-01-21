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
IF object_id('dbo.tig_schema_versions') IS NULL
create table [dbo].[tig_schema_versions] (
	-- Component Name
	[component] [varchar](100) NOT NULL,
	-- Version of loaded schema
	[version] [varchar](100) NOT NULL,
	-- Time when schema was loaded last time
	[last_update] [datetime] NOT NULL,
  CONSTRAINT [PK_tig_schema_versions] PRIMARY KEY CLUSTERED ( [component] ASC ) ON [PRIMARY]
);
-- QUERY END:


-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigSetComponentVersion')
  DROP PROCEDURE TigSetComponentVersion
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigGetComponentVersion')
  DROP FUNCTION TigGetComponentVersion
-- QUERY END:
GO


-- QUERY START:

create procedure TigGetComponentVersion (@_component nvarchar(255))
AS
  begin
    SELECT version FROM tig_schema_versions WHERE (component = @_component);
  end
-- QUERY END:
GO

-- QUERY START:
create procedure dbo.TigSetComponentVersion @_component nvarchar(255), @_version nvarchar(255)
AS
  BEGIN
    if exists (select 1 from dbo.tig_schema_versions where (component = @_component))
      begin
        UPDATE tig_schema_versions SET version = @_version, last_update = getutcdate() WHERE component = @_component;
      end
    else
      begin
        INSERT INTO tig_schema_versions (component, version, last_update) VALUES (@_component, @_version, getutcdate());
      end
  end
-- QUERY END:
GO
