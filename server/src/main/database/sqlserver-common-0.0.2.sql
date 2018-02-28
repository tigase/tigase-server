--
--  Tigase Jabber/XMPP Server
--  Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

-- QUERY START:
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
update tig_schema_versions
set version = concat(version, '-SNAPSHOT')
where exists (
    select 1
    from tig_schema_versions
    where
        component = 'common'
        and version = '0.0.1'
) and version not like '%-SNAPSHOT%';
-- QUERY END:
GO