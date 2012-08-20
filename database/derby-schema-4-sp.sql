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
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $
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
CREATE procedure TigUpdatePairs(nid bigint, uid bigint, tkey varchar(255), tval varchar(32672)) 
	PARAMETER STYLE JAVA
	LANGUAGE JAVA
	MODIFIES SQL DATA
	EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigUpdatePairs';
-- QUERY END:
