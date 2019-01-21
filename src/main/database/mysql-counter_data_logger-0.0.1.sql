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
create table if not exists tig_stats_log (
  lid            serial,
  ts             TIMESTAMP                 DEFAULT CURRENT_TIMESTAMP,
  hostname       varchar(2049)    NOT NULL,
  cpu_usage      double precision not null default 0,
  mem_usage      double precision not null default 0,
  uptime         bigint           not null default 0,
  vhosts         int              not null default 0,
  sm_packets     bigint           not null default 0,
  muc_packets    bigint           not null default 0,
  pubsub_packets bigint           not null default 0,
  c2s_packets    bigint           not null default 0,
  s2s_packets    bigint           not null default 0,
  ext_packets    bigint           not null default 0,
  presences      bigint           not null default 0,
  messages       bigint           not null default 0,
  iqs            bigint           not null default 0,
  registered     bigint           not null default 0,
  c2s_conns      int              not null default 0,
  s2s_conns      int              not null default 0,
  bosh_conns     int              not null default 0,
  primary key (ts, hostname(255))
);
-- QUERY END:

-- QUERY START:
call TigExecuteIfNot(
    (select count(1) from information_schema.COLUMNS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_stats_log' AND COLUMN_NAME = 'ws2s_conns'),
    "ALTER TABLE tig_stats_log ADD `ws2s_conns` INT not null default 0"
);
-- QUERY END:

-- QUERY START:
call TigExecuteIfNot(
    (select count(1) from information_schema.COLUMNS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_stats_log' AND COLUMN_NAME = 'ws2s_packets'),
    "ALTER TABLE tig_stats_log ADD `ws2s_packets` bigint not null default 0"
);
-- QUERY END:

-- QUERY START:
call TigExecuteIfNot(
    (select count(1) from information_schema.COLUMNS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_stats_log' AND COLUMN_NAME = 'sm_sessions'),
    "ALTER TABLE tig_stats_log ADD `sm_sessions` bigint not null default 0"
);
-- QUERY END:

-- QUERY START:
call TigExecuteIfNot(
    (select count(1) from information_schema.COLUMNS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tig_stats_log' AND COLUMN_NAME = 'sm_connections'),
    "ALTER TABLE tig_stats_log ADD `sm_connections` bigint not null default 0"
);
-- QUERY END:
