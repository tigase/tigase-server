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

--  To load schema to execute following commands:
--

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
CREATE TABLE [dbo].[tig_users](
	[uid] [bigint] IDENTITY(1,1) NOT NULL,

	-- Jabber User ID
	[user_id] [nvarchar](2049) NOT NULL,
	
	-- UserID SHA1 hash to prevent duplicate user_ids
	[sha1_user_id] [varbinary](32) NOT NULL,
	-- User password encrypted or not
	[user_pw] [nvarchar](255) NULL,
	-- Time the account has been created
	[acc_create_time] [datetime] DEFAULT getdate(),
	-- Time of the last user login
	[last_login] [datetime] default 0,
	-- Time of the last user logout
	[last_logout] [datetime] default 0,
	-- User online status, if > 0 then user is online, the value
	-- indicates the number of user connections.
	-- It is incremented on each user login and decremented on each
	-- user logout.
	[online_status] [int] default 0,
	-- Number of failed login attempts
	[failed_logins] [int] default 0,
	-- User status, whether the account is active or disabled
	-- >0 - account active, 0 - account disabled
	[account_status] [int] default 1,
	-- helper column for indexing due to limitation of SQL server 
	user_id_fragment AS LEFT (user_id, 256), 

	CONSTRAINT [PK_tig_users] PRIMARY KEY CLUSTERED ( [uid] ASC ) ON [PRIMARY],
	CONSTRAINT [IX_tig_users_sha1_user_id] UNIQUE NONCLUSTERED ( [sha1_user_id] ASC ) ON [PRIMARY]
) ON [PRIMARY]
-- QUERY END:

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_account_status] ON [dbo].[tig_users] ([account_status] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_last_login] ON [dbo].[tig_users] ([last_login] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_last_logout] ON [dbo].[tig_users] ( [last_logout] ASC)  ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_online_status] ON [dbo].[tig_users] ([online_status] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_user_id_fragment] ON [dbo].[tig_users] ( [user_id_fragment] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_users_user_pw] ON [dbo].[tig_users] ([user_pw] ASC) ON [PRIMARY]
-- QUERY END:
GO



-- QUERY START:
CREATE TABLE [dbo].[tig_nodes](
	[nid] [bigint] IDENTITY(1,1) NOT NULL,
	[parent_nid] [bigint] NULL,
	[uid] [bigint] NOT NULL,
	[node] [nvarchar](255) NOT NULL,
 CONSTRAINT [PK_tig_nodes_nid] PRIMARY KEY CLUSTERED ( [nid] ASC ) ON [PRIMARY],
 CONSTRAINT [IX_tnode] UNIQUE NONCLUSTERED ( [parent_nid] ASC, [uid] ASC, [node] ASC ) ON [PRIMARY]
) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_nodes_node] ON [dbo].[tig_nodes] ( [node] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_nodes_parent_nid] ON [dbo].[tig_nodes] ( [parent_nid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_nodes_uid] ON [dbo].[tig_nodes] ( [uid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_nodes]  WITH CHECK ADD  CONSTRAINT [FK_tig_nodes_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_nodes] CHECK CONSTRAINT [FK_tig_nodes_tig_users]
-- QUERY END:
GO


-- QUERY START:
CREATE TABLE [dbo].[tig_pairs](
	[nid] [bigint] NULL,
	[uid] [bigint] NOT NULL,
	[pkey] [nvarchar](255) NOT NULL,
	[pval] [ntext] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_pairs_nid] ON [dbo].[tig_pairs] ( [nid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_pairs_pkey] ON [dbo].[tig_pairs] ( [pkey] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_tig_pairs_uid] ON [dbo].[tig_pairs] ( [uid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_nodes] FOREIGN KEY([nid])
REFERENCES [dbo].[tig_nodes] ([nid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_nodes]
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_users]
-- QUERY END:
GO


-- QUERY START:
CREATE TABLE [dbo].[xmpp_stanza](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[stanza] [ntext] NOT NULL,
 CONSTRAINT [PK_xmpp_stanza] PRIMARY KEY CLUSTERED ( [id] ASC ) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
-- QUERY END:
GO


-- QUERY START:
CREATE TABLE [dbo].[short_news](
	[snid] [bigint] IDENTITY(1,1) NOT NULL,
	[publishing_time] [datetime] NOT NULL,
	[news_type] [varchar](50) NULL,
	[author] [nvarchar](128) NOT NULL,
	[subject] [nvarchar](128) NOT NULL,
	[body] [nvarchar](1024) NOT NULL,
 CONSTRAINT [PK_short_news_snid] PRIMARY KEY CLUSTERED ( [snid] ASC ) ON [PRIMARY]
) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_short_news_author] ON [dbo].[short_news] ( [author] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_short_news_news_type] ON [dbo].[short_news] ( [news_type] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
CREATE NONCLUSTERED INDEX [IX_short_news_publishing_time] ON [dbo].[short_news] ( [publishing_time] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[short_news] ADD  CONSTRAINT [DF_short_news_publishing_time]  DEFAULT (getdate()) FOR [publishing_time]
-- QUERY END:
GO

