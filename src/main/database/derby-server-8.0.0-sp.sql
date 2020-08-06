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
CREATE function TigGetDBProperty(tkey varchar(255)) returns long varchar
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigGetDBProperty';
-- QUERY END:

-- QUERY START:
CREATE procedure TigPutDBProperty(tkey varchar(255), tval varchar(32672))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigPutDBProperty';
-- QUERY END:

-- QUERY START:
CREATE procedure TigInitdb()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigInitdb';
-- QUERY END:

-- QUERY START:
CREATE procedure TigAddUser(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAddUser';
-- QUERY END:

-- QUERY START:
CREATE procedure TigAddUserPlainPw(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAddUserPlainPw';
-- QUERY END:

-- QUERY START:
CREATE procedure TigGetUserDBUid(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigGetUserDBUid';
-- QUERY END:

-- QUERY START:
CREATE procedure TigRemoveUser(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigRemoveUser';
-- QUERY END:

-- QUERY START:
CREATE procedure TigGetPassword(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigGetPassword';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUpdatePasswordPlainPwRev(userId varchar(2049), userPw varchar(255))
  PARAMETER STYLE JAVA
  LANGUAGE JAVA
  MODIFIES SQL DATA
  EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePasswordPlainPwRev';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUpdatePasswordPlainPw(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePasswordPlainPw';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUpdatePassword(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePassword';
-- QUERY END:

-- QUERY START:
CREATE procedure TigOnlineUsers()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigOnlineUsers';
-- QUERY END:

-- QUERY START:
CREATE procedure TigOfflineUsers()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigOfflineUsers';
-- QUERY END:

-- QUERY START:
CREATE procedure TigAllUsers()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAllUsers';
-- QUERY END:

-- QUERY START:
CREATE procedure TigAllUsersCount()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAllUsersCount';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUserLoginPlainPw(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserLoginPlainPw';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUserLogin(userId varchar(2049), userPw varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserLogin';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUserLogout(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserLogout';
-- QUERY END:

-- QUERY START:
CREATE procedure TigDisableAccount(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigDisableAccount';
-- QUERY END:

-- QUERY START:
CREATE procedure TigEnableAccount(userId varchar(2049))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigEnableAccount';
-- QUERY END:

-- QUERY START:
CREATE procedure TigActiveAccounts()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigActiveAccounts';
-- QUERY END:

-- QUERY START:
CREATE procedure TigDisabledAccounts()
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	READS SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigDisabledAccounts';
-- QUERY END:

-- QUERY START:
CREATE procedure TigAddNode(parentNid bigint, uid bigint, node varchar(255))
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	DYNAMIC RESULT SETS 1
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigAddNode';
-- QUERY END:

-- QUERY START:
CREATE procedure TigUpdatePairs(nid bigint, uid bigint, tkey varchar(255), tval clob)
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePairs';
-- QUERY END:

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


-- ------------- Credentials support
-- QUERY START:
CREATE PROCEDURE TigUserCredential_Update(userId varchar(2049), username varchar(2049), mechanism varchar(128), value varchar(32672))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserCredentialUpdate';
-- QUERY END:

-- QUERY START:
CREATE PROCEDURE TigUserCredentials_Get(userId varchar(2049), username varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserCredentialsGet';
-- QUERY END:

-- QUERY START:
CREATE PROCEDURE TigUserUsernames_Get(userId varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
DYNAMIC RESULT SETS 1
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserUsernamesGet';
-- QUERY END:

-- QUERY START:
CREATE PROCEDURE TigUserCredential_Remove(userId varchar(2049), username varchar(2049))
PARAMETER STYLE JAVA
LANGUAGE JAVA
MODIFIES SQL DATA
EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUserCredentialRemove';
-- QUERY END:
