--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
-- Database stored procedures and functions for Tigase schema version 7.2.0

-- LOAD FILE: database/sqlserver-schema-7-2-schema.sql

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sysobjects where name = 'msg_history' and xtype = 'U')
    exec sp_rename 'dbo.msg_history', 'tig_offline_messages';
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.columns where object_id = object_id('dbo.tig_offline_messages') and name = 'message' and user_type_id = (select user_type_id from  sys.types where name = 'nvarchar') and max_length = 8000)
    alter table tig_offline_messages alter column message nvarchar(max) not null;
-- QUERY END:
GO
-- QUERY START:
if not exists (select 1 from sys.columns where object_id = object_id('dbo.tig_offline_messages') and name = 'receiver')
begin
    alter table tig_offline_messages add receiver nvarchar(2049);
    alter table tig_offline_messages add receiver_sha1 varbinary(20);
    alter table tig_offline_messages add sender nvarchar(2049);
    alter table tig_offline_messages add sender_sha1 varbinary(20);
end
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'index_expired' )
    drop index index_expired on [dbo].[tig_offline_messages];
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'index_receiver_uid_sender_uid' )
    drop index index_receiver_uid_sender_uid on [dbo].[tig_offline_messages];
-- QUERY END:
GO
-- QUERY START:
if exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'index_sender_uid_receiver_uid' )
    drop index index_sender_uid_receiver_uid on [dbo].[tig_offline_messages];
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.user_jid') is not null
    update tig_offline_messages
    set receiver = (select jid from user_jid where jid_id = receiver_uid),
        sender = (select jid from user_jid where jid_id = sender_uid)
    where receiver is null;
-- QUERY END:
GO

-- QUERY START:
update tig_offline_messages
set receiver_sha1 = HASHBYTES('SHA1', lower(receiver)),
    sender_sha1 = HASHBYTES('SHA1', lower(sender))
where receiver_sha1 is null;
-- QUERY END:
GO

-- QUERY START:
if exists (select 1 from sys.columns where object_id = object_id('dbo.tig_offline_messages') and name = 'receiver_uid')
begin
    alter table tig_offline_messages drop column receiver_uid;
    alter table tig_offline_messages drop column sender_uid;
end
-- QUERY END:
GO

-- QUERY START:
alter table tig_offline_messages alter column receiver nvarchar(max) not null;
-- QUERY END:
GO
-- QUERY START:
alter table tig_offline_messages alter column receiver_sha1 varbinary(20) not null;
-- QUERY END:
GO

-- QUERY START:
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name like 'PK_%' )
    alter table tig_offline_messages add primary key (msg_id);
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.broadcast_msgs') is not null
begin
    exec sp_rename 'dbo.broadcast_msgs', 'tig_broadcast_messages';
    alter table tig_broadcast_messages alter column msg nvarchar(max) not null;
end
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
if object_id('dbo.user_jid') is not null and object_id('dbo.broadcast_msgs_recipients') is not null
    insert into tig_broadcast_jids (jid, jid_sha1)
        select u.jid, HASHBYTES('SHA1', lower(u.jid))
        from user_jid u
        inner join broadcast_msgs_recipients b on u.jid_id = b.jid_id
        where not exists (select 1 from tig_broadcast_jids bj where bj.jid = u.jid);
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
if object_id('dbo.user_jid') is not null and object_id('dbo.broadcast_msgs_recipients') is not null
    insert into tig_broadcast_recipients (msg_id, jid_id)
        select x.msg_id, x.jid_id
        from (select bmr.msg_id, bj.jid_id
            from broadcast_msgs_recipients bmr
            inner join user_jid uj on uj.jid_id = bmr.jid_id
            inner join tig_broadcast_jids bj on bj.jid_sha1 = HASHBYTES('SHA1', lower(uj.jid))
        ) x
        where not exists (select 1 from tig_broadcast_recipients br where br.msg_id = x.msg_id and br.jid_id = x.jid_id);
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.broadcast_msgs_recipients') is not null
    drop table broadcast_msgs_recipients;
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.user_jid') is not null
    drop table user_jid;
-- QUERY END:
GO

-- QUERY START:
if object_id('dbo.tig_offline_messages') is null
    create table tig_offline_messages (
        msg_id [bigint] IDENTITY(1,1),
        ts [datetime] DEFAULT getdate(),
        expired [datetime],
        sender nvarchar(2049),
        sender_sha1 varbinary(20),
        receiver nvarchar(2049) not null,
        receiver_sha1 varbinary(20) not null,
	    msg_type int not null default 0,
	    message nvarchar(max),

	    primary key (msg_id)
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
