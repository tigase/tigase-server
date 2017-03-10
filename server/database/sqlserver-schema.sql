SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tig_max_ids](
	[max_uid] [bigint] NULL,
	[max_nid] [bigint] NULL
) ON [PRIMARY]

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tig_users](
	[uid] [bigint] NOT NULL,
	[user_id] [nvarchar](128) NOT NULL
) ON [PRIMARY]

GO

CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_users] ON [dbo].[tig_users] 
(
	[user_id] ASC
) ON [PRIMARY]
GO

CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_users_1] ON [dbo].[tig_users] 
(
	[uid] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[short_news](
	[snid] [bigint] IDENTITY(1,1) NOT NULL,
	[publishing_time] [datetime] NOT NULL CONSTRAINT [DF_short_news_publishing_time]  DEFAULT (getdate()),
	[news_type] [nvarchar](50) NULL,
	[author] [nvarchar](128) NOT NULL,
	[subject] [nvarchar](128) NOT NULL,
	[body] [nvarchar](1024) NOT NULL,
 CONSTRAINT [PK_short_news] PRIMARY KEY CLUSTERED 
(
	[snid] ASC
) ON [PRIMARY]
) ON [PRIMARY]

GO

CREATE NONCLUSTERED INDEX [IX_short_news] ON [dbo].[short_news] 
(
	[publishing_time] ASC
) ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX [IX_short_news_1] ON [dbo].[short_news] 
(
	[author] ASC
) ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX [IX_short_news_2] ON [dbo].[short_news] 
(
	[news_type] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[xmpp_stanza](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[stanza] [text] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tig_nodes](
	[nid] [bigint] NOT NULL,
	[parent_nid] [bigint] NULL,
	[uid] [bigint] NOT NULL,
	[node] [nvarchar](64) NOT NULL
) ON [PRIMARY]

GO

CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_nodes] ON [dbo].[tig_nodes] 
(
	[parent_nid] ASC,
	[node] ASC,
	[uid] ASC
) ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX [IX_tig_nodes_1] ON [dbo].[tig_nodes] 
(
	[node] ASC
) ON [PRIMARY]
GO

CREATE UNIQUE NONCLUSTERED INDEX [IX_tig_nodes_2] ON [dbo].[tig_nodes] 
(
	[nid] ASC
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tig_pairs](
	[nid] [bigint] NOT NULL,
	[uid] [bigint] NOT NULL,
	[pkey] [nvarchar](128) NOT NULL,
	[pval] [text] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO

CREATE NONCLUSTERED INDEX [IX_tig_pairs] ON [dbo].[tig_pairs] 
(
	[pkey] ASC
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[tig_nodes]  WITH CHECK ADD  CONSTRAINT [FK_tig_nodes_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
GO
ALTER TABLE [dbo].[tig_nodes] CHECK CONSTRAINT [FK_tig_nodes_tig_users]
GO
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_nodes] FOREIGN KEY([nid])
REFERENCES [dbo].[tig_nodes] ([nid])
GO
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_nodes]
GO
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
GO
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_users]
