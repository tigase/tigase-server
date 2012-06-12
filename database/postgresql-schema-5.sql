--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
--
--  This program is free software: you can redistribute it and/or modify
--  it under the terms of the GNU General Public License as published by
--  the Free Software Foundation, either version 3 of the License.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU General Public License for more details.
--
--  You should have received a copy of the GNU General Public License
--  along with this program. Look for COPYING file in the top folder.
--  If not, see http://www.gnu.org/licenses/.
--
--  $Rev: $
--  Last modified by $Author: $
--  $Date: $
--

--  To load schema to PostgreSQL database execute following commands:
--
--  createuser tigase
--  createdb -U tigase tigase
--  psql -q -U tigase -d tigase -f postgresql-schema.sql

-- QUERY START:
\i database/postgresql-schema-4-schema.sql
-- QUERY END:

-- QUERY START:
\i database/postgresql-schema-4-sp.sql
-- QUERY END:

-- QUERY START:
\i database/postgresql-schema-4-props.sql
-- QUERY END:
