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

-- QUERY START: create database

create database ${dbName};

-- QUERY END: create database

-- QUERY START: add user

GRANT ALL ON ${dbName}.* TO ${dbUser}@'%' IDENTIFIED BY '${dbPass}';

-- QUERY END: add user

-- QUERY START: add user

GRANT ALL ON ${dbName}.* TO ${dbUser}@'localhost' IDENTIFIED BY '${dbPass}';

-- QUERY END: add user

-- QUERY START: add user

GRANT ALL ON ${dbName}.* TO ${dbUser} IDENTIFIED BY '${dbPass}';

-- QUERY END: user

-- QUERY START: update user privileges

GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${dbUser}@'localhost';

-- QUERY END: update user privileges

-- QUERY START: update user privileges

GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${dbUser}@'%';

-- QUERY END: update user privileges

-- QUERY START: update user privileges

GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${dbUser};

-- QUERY END: update user privileges

-- QUERY START: flush privileges

FLUSH PRIVILEGES;

-- QUERY END: flush privileges

