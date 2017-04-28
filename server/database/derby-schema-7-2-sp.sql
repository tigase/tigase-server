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
--

run 'database/derby-schema-7-1-sp.sql';

-- LOAD FILE: database/derby-schema-7-1-sp.sql

-- QUERY START:
CREATE procedure TigUpdateLoginTime(userId varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdateLoginTime';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_AddMessage("to" varchar(2049), "from" varchar(2049), "type" int, "ts" timestamp, "message" varchar(32672), "expired" timestamp, "limit" bigint)
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.addMessage';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_GetMessages("to" varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.getMessages';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_GetMessagesByIds("to" varchar(2049), "msg_id1" varchar(50), "_msg_id2" varchar(50), "_msg_id3" varchar(50), "_msg_id4" varchar(50))
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.getMessagesByIds';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_GetMessagesCount("to" varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.getMessagesCount';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_ListMessages("to" varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.listMessages';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_DeleteMessages("to" varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.deleteMessages';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_DeleteMessagesByIds("to" varchar(2049), "msg_id1" varchar(50), "_msg_id2" varchar(50), "_msg_id3" varchar(50), "_msg_id4" varchar(50))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.deleteMessagesByIds';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_DeleteMessage(msg_id bigint)
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.deleteMessage';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_GetExpiredMessages("limit" int)
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.getExpiredMessages';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_OfflineMessages_GetExpiredMessagesBefore("before" timestamp)
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgRepositoryStoredProcedures.getExpiredMessagesBefore';
-- QUERY END:


-- ------------ Broadcast Messages
-- QUERY START:
CREATE procedure Tig_BroadcastMessages_AddMessage("msg_id" varchar(128), "expired" timestamp, "msg" varchar(32672))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.MsgBroadcastRepositoryStoredProcedures.addMessage';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_BroadcastMessages_AddMessageRecipient("msg_id" varchar(128), "jid" varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.MsgBroadcastRepositoryStoredProcedures.addMessageRecipient';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_BroadcastMessages_GetMessages("expired" timestamp)
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgBroadcastRepositoryStoredProcedures.getMessages';
-- QUERY END:

-- QUERY START:
CREATE procedure Tig_BroadcastMessages_GetMessageRecipients("msg_id" varchar(128))
PARAMETER STYLE JAVA
LANGUAGE JAVA
READS SQL DATA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.MsgBroadcastRepositoryStoredProcedures.getMessageRecipients';
-- QUERY END:

-- QUERY START:
CREATE PROCEDURE TigUpdateAccountStatus("userId" VARCHAR(2049), "status" INT)
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdateAccountStatus';
-- QUERY END:

-- QUERY START:
CREATE PROCEDURE TigAccountStatus("userId" VARCHAR(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAccountStatus';
-- QUERY END:
