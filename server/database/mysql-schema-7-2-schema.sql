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
--

-- Database stored procedures and functions for Tigase schema version 5.1

source database/mysql-schema-7-1-schema.sql;

-- LOAD FILE: database/mysql-schema-7-1-schema.sql


-- QUERY START:
alter table tig_pairs modify `pval` mediumtext character set utf8mb4 collate utf8mb4_unicode_ci;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigUpgradeMsgHistory;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigUpgradeMsgHistory()
begin
    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'msg_history') then
        alter table msg_history rename tig_offline_messages;
    else
        create table if not exists tig_offline_messages (
            msg_id bigint unsigned not null auto_increment,
            ts timestamp(6) default current_timestamp(6),
            expired timestamp null default null,
            sender varchar(2049),
            sender_sha1 char(128),
            receiver varchar(2049) not null,
            receiver_sha1 char(128),
            msg_type int not null default 0,
            message mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null,
            primary key (msg_id),
            key tig_offline_messages_expired_index (expired),
            key tig_offline_messages_receiver_sha1_index (receiver_sha1),
            key tig_offline_messages_receiver_sha1_sender_sha1_index (receiver_sha1, sender_sha1)
        ) ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;        
    end if;

    if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'expired') then
        alter table tig_offline_messages modify expired timestamp(6) null default null;
    end if;

    if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'ts') then
        alter table tig_offline_messages modify ts timestamp(6) default current_timestamp(6);
    end if;

    if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'message') then
        alter table tig_offline_messages modify message mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null;
    end if;

    if not exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'receiver') then
        alter table tig_offline_messages
            add receiver varchar(2049) character set utf8,
            add receiver_sha1 char(128),
            add sender varchar(2049) character set utf8,
            add sender_sha1 char(128),
            add key tig_offline_messages_expired_index (expired),
            add key tig_offline_messages_receiver_sha1_index (receiver_sha1),
            add key tig_offline_messages_receiver_sha1_sender_sha1_index (receiver_sha1, sender_sha1),
            drop index expired,
            drop index sender_uid,
            drop index receiver_uid;
    end if;

	if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'user_jid')
	        and exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'receiver_uid') then
    	update tig_offline_messages
    	set receiver = (select jid from user_jid where jid_id = receiver_uid),
        	sender = (select jid from user_jid where jid_id = sender_uid)
    	where receiver is null;
    end if;

    update tig_offline_messages
    set receiver_sha1 = sha1(lower(receiver)),
        sender_sha1 = sha1(lower(sender))
    where receiver_sha1 is null;

    alter table tig_offline_messages
        modify receiver varchar(2049) character set utf8 not null,
        modify receiver_sha1 char(128) not null;

    if exists (select 1 from information_schema.columns where table_schema = database() and table_name = 'tig_offline_messages' and column_name = 'receiver_uid') then
        alter table tig_offline_messages
            drop column sender_uid,
            drop column receiver_uid;
    end if;

    if not exists (select 1 from information_schema.statistics where table_schema = database() and table_name = 'tig_offline_messages' and index_name = 'PRIMARY') then
        alter table tig_offline_messages add primary key (msg_id);
    end if;

    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'broadcast_msgs') then
        alter table broadcast_msgs rename tig_broadcast_messages;
        alter table tig_broadcast_messages
            modify expired timestamp(6) not null,
            modify msg mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null;
    else
        create table if not exists tig_broadcast_messages (
            id varchar(128) not null,
            expired timestamp(6) not null,
			msg mediumtext character set utf8mb4 collate utf8mb4_unicode_ci not null,
			primary key (id)
			);
    end if;

    create table if not exists tig_broadcast_jids (
        jid_id bigint unsigned not null auto_increment,
        jid varchar(2049) not null,
        jid_sha1 char(128) not null,

        primary key (jid_id)
    );
    if not exists (select 1 from information_schema.statistics where table_schema = database() and table_name = 'tig_broadcast_jids' and index_name = 'tig_broadcast_jids_jid_sha1') then
        create index tig_broadcast_jids_jid_sha1 on tig_broadcast_jids (jid_sha1);
    end if;

    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'user_jid')
            and exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'public.broadcast_msgs_recipients') then
        insert into tig_broadcast_jids (jid, jid_sha1)
            select u.jid, sha1(lower(u.jid))
            from user_jid u
            inner join broadcast_msgs_recipients b on u.jid_id = b.jid_id
            where not exists (select 1 from tig_broadcast_jids bj where bj.jid = u.jid);
    end if;

    create table if not exists tig_broadcast_recipients (
        msg_id varchar(128) not null references tig_broadcast_messages(id),
        jid_id bigint not null references tig_broadcast_jids(jid_id),
        primary key (msg_id, jid_id)
    );

    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'user_jid')
            and exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'public.broadcast_msgs_recipients') then
        insert into tig_broadcast_recipients (msg_id, jid_id)
            select x.msg_id, x.jid_id
            from (select bmr.msg_id, bj.jid_id
                from broadcast_msgs_recipients bmr
                inner join user_jid uj on uj.jid_id = bmr.jid_id
                inner join tig_broadcast_jids bj on bj.jid_sha1 = sha1(lower(uj.jid))
            ) x
            where not exists (select 1 from tig_broadcast_recipients br where br.msg_id = x.msg_id and br.jid_id = x.jid_id);
    end if;

    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'public.broadcast_msgs_recipients') then
        drop table broadcast_msgs_recipients;
    end if;

    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'user_jid') then
        drop table user_jid;
    end if;

end //
-- QUERY END:

delimiter ;

-- QUERY START:
call TigUpgradeMsgHistory();
-- QUERY END:

-- QUERY START:
drop procedure if exists TigUpgradeMsgHistory;
-- QUERY END:

-- ------------ Clustering support

-- QUERY START:
create table if not exists tig_cluster_nodes (
    hostname varchar(255) not null,
    secondary varchar(512),
    password varchar(255),
    last_update timestamp(6) default current_timestamp(6) on update current_timestamp(6),
    port int,
    cpu_usage double precision unsigned not null,
    mem_usage double precision unsigned not null,
    primary key (hostname)
) ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
-- QUERY END:

