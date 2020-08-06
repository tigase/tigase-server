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
IF object_id('dbo.tig_users') IS NULL
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
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_account_status' )
CREATE NONCLUSTERED INDEX [IX_tig_users_account_status] ON [dbo].[tig_users] ([account_status] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_last_login' )
CREATE NONCLUSTERED INDEX [IX_tig_users_last_login] ON [dbo].[tig_users] ([last_login] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_last_logout' )
CREATE NONCLUSTERED INDEX [IX_tig_users_last_logout] ON [dbo].[tig_users] ( [last_logout] ASC)  ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_online_status' )
CREATE NONCLUSTERED INDEX [IX_tig_users_online_status] ON [dbo].[tig_users] ([online_status] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_user_id_fragment' )
CREATE NONCLUSTERED INDEX [IX_tig_users_user_id_fragment] ON [dbo].[tig_users] ( [user_id_fragment] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_users') and name = 'IX_tig_users_user_pw' )
CREATE NONCLUSTERED INDEX [IX_tig_users_user_pw] ON [dbo].[tig_users] ([user_pw] ASC) ON [PRIMARY]
-- QUERY END:
GO



-- QUERY START:
IF object_id('dbo.tig_nodes') IS NULL
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
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_nodes') and name = 'IX_tig_nodes_node' )
CREATE NONCLUSTERED INDEX [IX_tig_nodes_node] ON [dbo].[tig_nodes] ( [node] ASC) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_nodes') and name = 'IX_tig_nodes_parent_nid' )
CREATE NONCLUSTERED INDEX [IX_tig_nodes_parent_nid] ON [dbo].[tig_nodes] ( [parent_nid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_nodes') and name = 'IX_tig_nodes_uid' )
CREATE NONCLUSTERED INDEX [IX_tig_nodes_uid] ON [dbo].[tig_nodes] ( [uid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.FK_tig_nodes_tig_users') is null
ALTER TABLE [dbo].[tig_nodes]  WITH CHECK ADD  CONSTRAINT [FK_tig_nodes_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_nodes] CHECK CONSTRAINT [FK_tig_nodes_tig_users]
-- QUERY END:
GO


-- QUERY START:
IF object_id('dbo.tig_pairs') IS NULL
CREATE TABLE [dbo].[tig_pairs](
	[pid] [bigint] IDENTITY(1,1) NOT NULL,
	[nid] [bigint] NULL,
	[uid] [bigint] NOT NULL,
	[pkey] [nvarchar](255) NOT NULL,
	[pval] [ntext] NULL,
    CONSTRAINT [PK_tig_pairs] PRIMARY KEY CLUSTERED ( [pid] ASC ) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_pairs') and name = 'IX_tig_pairs_nid' )
CREATE NONCLUSTERED INDEX [IX_tig_pairs_nid] ON [dbo].[tig_pairs] ( [nid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_pairs') and name = 'IX_tig_pairs_pkey' )
CREATE NONCLUSTERED INDEX [IX_tig_pairs_pkey] ON [dbo].[tig_pairs] ( [pkey] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_pairs') and name = 'IX_tig_pairs_uid' )
CREATE NONCLUSTERED INDEX [IX_tig_pairs_uid] ON [dbo].[tig_pairs] ( [uid] ASC ) ON [PRIMARY]
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.FK_tig_pairs_tig_nodes') is null
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_nodes] FOREIGN KEY([nid])
REFERENCES [dbo].[tig_nodes] ([nid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_nodes]
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.FK_tig_pairs_tig_users') is null
ALTER TABLE [dbo].[tig_pairs]  WITH CHECK ADD  CONSTRAINT [FK_tig_pairs_tig_users] FOREIGN KEY([uid])
REFERENCES [dbo].[tig_users] ([uid])
-- QUERY END:
GO

-- QUERY START:
ALTER TABLE [dbo].[tig_pairs] CHECK CONSTRAINT [FK_tig_pairs_tig_users]
-- QUERY END:
GO

-- QUERY START:
IF object_id('dbo.tig_offline_messages') IS NULL
    create table tig_offline_messages (
        msg_id [bigint] IDENTITY(1,1),
        ts [datetime] DEFAULT GETUTCDATE(),
        expired [datetime],
        sender nvarchar(2049),
        sender_sha1 varbinary(20),
        receiver nvarchar(2049) not null,
        receiver_sha1 varbinary(20) not null,
        msg_type int not null default 0,
        message nvarchar(max) not null,

        primary key (msg_id)
    );
-- QUERY END:
GO
-- QUERY START:
if object_id('dbo.tig_broadcast_messages') is null
    create table tig_broadcast_messages (
		id varchar(128) not null,
	    expired datetime not null,
	    msg nvarchar(max) not null,
	    primary key (id)
	);
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.tig_broadcast_jids') is null
    create table tig_broadcast_jids (
        jid_id [bigint] IDENTITY(1,1),
        jid [nvarchar](2049) not null,
        jid_sha1 [varbinary](20) not null,

        primary key (jid_id)
    );
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_broadcast_jids_jid_sha1' )
    create index IX_tig_broadcast_jids_jid_sha1 on tig_broadcast_jids (jid_sha1);
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.tig_broadcast_recipients') is null
    create table tig_broadcast_recipients (
        msg_id [varchar](128) not null references tig_broadcast_messages(id),
        jid_id [bigint] not null references tig_broadcast_jids(jid_id),
        primary key (msg_id, jid_id)
    );
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_offline_messages_expired' )
    create index IX_tig_offline_messages_expired on [dbo].[tig_offline_messages] (expired);
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_offline_messages_receiver_sha1' )
    create index IX_tig_offline_messages_receiver_sha1 on [dbo].[tig_offline_messages] (receiver_sha1);
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_offline_messages_receiver_sha1_sender_sha1' )
    create index IX_tig_offline_messages_receiver_sha1_sender_sha1 on [dbo].[tig_offline_messages] (receiver_sha1, sender_sha1);
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.tig_cluster_nodes') is null
    create table [dbo].[tig_cluster_nodes] (
        hostname nvarchar(450) not null,
        secondary nvarchar(512),
        password nvarchar(255) not null,
		last_update [datetime] default getutcdate(),
		port int,
		cpu_usage double precision not null,
		mem_usage double precision not null,

		constraint [PK_tig_cluster_nodes] PRIMARY KEY ClUSTERED ( [hostname] asc ) on [PRIMARY],
		constraint [IX_tig_cluster_nodes_hostname] unique nonclustered ( [hostname] asc) on [PRIMARY]
    );
-- QUERY END:
GO

-- ------------- Credentials support
-- QUERY START:
if object_id('dbo.tig_user_credentials') is null
    create table tig_user_credentials (
        uid bigint not null references tig_users(uid),
        username nvarchar(2049) not null,
        username_sha1 varbinary(32) not null,
        mechanism nvarchar(128) not null,
        value nvarchar(max) not null,

        primary key (uid, username_sha1, mechanism)
    );
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from tig_user_credentials) and exists (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'TigGetDBProperty')
begin
    execute('insert into tig_user_credentials (uid, username, username_sha1, mechanism, value)
        select uid, ''default'', HASHBYTES(''SHA1'', ''default''), coalesce(TigGetDBProperty(''password-encoding''), ''PLAIN''), user_pw
        from tig_users
        where
            user_pw is not null;

    update tig_users set user_pw = null where user_pw is not null;');
end
-- QUERY END:
GO
