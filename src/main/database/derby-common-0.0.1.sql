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
create table tig_schema_versions (
	-- Component Name
	component varchar(100) NOT NULL,
	-- Version of loaded schema
	version varchar(100) NOT NULL,
	-- Time when schema was loaded last time
	last_update timestamp NOT NULL
);
-- QUERY END:

-- QUERY START:
create unique index component on tig_schema_versions ( component );
-- QUERY END:


-- QUERY START:
CREATE procedure TigGetComponentVersion(component varchar(100))
  PARAMETER STYLE JAVA
  LANGUAGE JAVA
  MODIFIES SQL DATA
  DYNAMIC RESULT SETS 1
  EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigGetComponentVersion';
-- QUERY END:

-- QUERY START:
CREATE procedure TigSetComponentVersion(component varchar(100), version varchar(100))
  PARAMETER STYLE JAVA
  LANGUAGE JAVA
  MODIFIES SQL DATA
  EXTERNAL NAME 'tigase.db.derby.StoredProcedures.tigSetComponentVersion';
-- QUERY END: