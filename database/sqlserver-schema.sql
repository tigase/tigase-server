SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[tig_max_ids]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[tig_max_ids](
	[max_uid] [bigint] NULL,
	[max_nid] [bigint] NULL
) ON [PRIMARY]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[short_news]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[short_news](
	[snid] [bigint] IDENTITY(1,1) NOT NULL,
	[publishing_time] [datetime] NOT NULL CONSTRAINT [DF_short_news_publishing_time]  DEFAULT (getdate()),
	[news_type] [varchar](50) NULL,
	[author] [varchar](128) NOT NULL,
	[subject] [varchar](128) NOT NULL,
	[body] [varchar](1024) NOT NULL,
 CONSTRAINT [PK_short_news] PRIMARY KEY CLUSTERED 
(
	[snid] ASC
) ON [PRIMARY]
) ON [PRIMARY]
END
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[short_news]') AND name = N'IX_short_news')
CREATE NONCLUSTERED INDEX [IX_short_news] ON [dbo].[short_news] 
(
	[publishing_time] ASC
) ON [PRIMARY]
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[short_news]') AND name = N'IX_short_news_1')
CREATE NONCLUSTERED INDEX [IX_short_news_1] ON [dbo].[short_news] 
(
	[author] ASC
) ON [PRIMARY]
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[short_news]') AND name = N'IX_short_news_2')
CREATE NONCLUSTERED INDEX [IX_short_news_2] ON [dbo].[short_news] 
(
	[news_type] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[xmpp_stanza]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[xmpp_stanza](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[stanza] [text] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[tig_users]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[tig_users](
	[uid] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [varchar](128) NOT NULL
) ON [PRIMARY]
END
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_users]') AND name = N'IX_tig_users')
CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_users] ON [dbo].[tig_users] 
(
	[user_id] ASC
) ON [PRIMARY]
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_users]') AND name = N'IX_tig_users_1')
CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_users_1] ON [dbo].[tig_users] 
(
	[uid] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[tig_pairs]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[tig_pairs](
	[nid] [bigint] NOT NULL,
	[uid] [bigint] NOT NULL,
	[pkey] [varchar](128) NOT NULL,
	[pval] [text] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_pairs]') AND name = N'IX_tig_pairs')
CREATE NONCLUSTERED INDEX [IX_tig_pairs] ON [dbo].[tig_pairs] 
(
	[pkey] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[tig_nodes]') AND OBJECTPROPERTY(id, N'IsUserTable') = 1)
BEGIN
CREATE TABLE [dbo].[tig_nodes](
	[nid] [bigint] IDENTITY(1,1) NOT NULL,
	[parent_nid] [bigint] NULL,
	[uid] [bigint] NOT NULL,
	[node] [varchar](64) NOT NULL
) ON [PRIMARY]
END
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_nodes]') AND name = N'IX_tig_nodes')
CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_nodes] ON [dbo].[tig_nodes] 
(
	[parent_nid] ASC,
	[node] ASC
) ON [PRIMARY]
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_nodes]') AND name = N'IX_tig_nodes_1')
CREATE NONCLUSTERED INDEX [IX_tig_nodes_1] ON [dbo].[tig_nodes] 
(
	[node] ASC
) ON [PRIMARY]
GO

IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE id = OBJECT_ID(N'[dbo].[tig_nodes]') AND name = N'IX_tig_nodes_2')
CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_nodes_2] ON [dbo].[tig_nodes] 
(
	[nid] ASC
) ON [PRIMARY]
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_tig_pairs_tig_nodes]') AND type = 'F')
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_nodes] FOREIGN KEY([nid])
REFERENCES [dbo].[tig_nodes] ([nid])
GO
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_nodes]
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_tig_pairs_tig_users]') AND type = 'F')
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
GO
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_users]
GO
IF NOT EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[dbo].[FK_tig_nodes_tig_users]') AND type = 'F')
ALTER TABLE [dbo].[tig_nodes]  WITH CHECK ADD  CONSTRAINT [FK_tig_nodes_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
GO
ALTER TABLE [dbo].[tig_nodes] CHECK CONSTRAINT [FK_tig_nodes_tig_users]

insert into tig_max_ids (max_uid, max_nid) values (1, 1);
