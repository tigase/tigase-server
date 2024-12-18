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
if not exists (select 1 from sys.indexes where object_id = object_id('dbo.tig_offline_messages') and name = 'IX_tig_offline_messages_receiver_sha1_msg_type' )
    create index IX_tig_offline_messages_receiver_sha1_msg_type on [dbo].[tig_offline_messages] (receiver_sha1, msg_type);
-- QUERY END:
GO
