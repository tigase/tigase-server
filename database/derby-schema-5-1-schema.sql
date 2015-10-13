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

run 'database/derby-schema-4-schema.sql';

-- LOAD FILE: database/derby-schema-4-schema.sql

-- QUERY START:
alter table tig_pairs ADD column pval2 CLOB;
-- QUERY END:

-- QUERY START:
update tig_pairs set pval2=pval;
-- QUERY END:

-- QUERY START:
alter table tig_pairs drop column pval;
-- QUERY END:

-- QUERY START:
RENAME COLUMN tig_pairs.pval2 to pval;
-- QUERY END:
