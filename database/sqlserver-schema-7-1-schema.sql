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

--  To load schema to execute following commands:
--

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- LOAD FILE: database/sqlserver-schema-5-1-schema.sql


-- QUERY START:
CREATE UNIQUE CLUSTERED INDEX [IX_clustered_tig_pairs_nid_uid_pkey] ON [dbo].[tig_pairs]
(
	[nid] ASC,
	[uid] ASC,
	[pkey] ASC
) ON [PRIMARY]
-- QUERY END:
GO

