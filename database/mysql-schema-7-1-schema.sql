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

-- Database stored procedures and functions for Tigase schema version 5.1

source database/mysql-schema-5-1-schema.sql;

-- LOAD FILE: database/mysql-schema-5-1-schema.sql


-- QUERY START:
ALTER TABLE tig_pairs DROP FOREIGN KEY tig_pairs_constr_1;
-- QUERY END:

-- QUERY START:
ALTER TABLE tig_pairs DROP FOREIGN KEY tig_pairs_constr_2;
-- QUERY END:

-- QUERY START:
ALTER TABLE tig_pairs ADD PRIMARY KEY(nid,uid,pkey);
-- QUERY END:

-- QUERY START:
ALTER TABLE tig_pairs ADD CONSTRAINT tig_pairs_constr_1 FOREIGN KEY (uid) REFERENCES tig_users (uid);
-- QUERY END:

-- QUERY START:
ALTER TABLE tig_pairs ADD CONSTRAINT tig_pairs_constr_2 FOREIGN KEY (nid) REFERENCES tig_nodes (nid);
-- QUERY END:
