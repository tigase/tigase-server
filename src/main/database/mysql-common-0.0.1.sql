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
create table if not EXISTS tig_schema_versions (
	-- Component Name
	component varchar(100) NOT NULL,
	-- Version of loaded schema
	version varchar(100) NOT NULL,
	-- Time when schema was loaded last time
	last_update timestamp NOT NULL,
	primary key (component)
);
-- QUERY END:

-- QUERY START:
drop PROCEDURE if exists TigGetComponentVersion;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigSetComponentVersion;
-- QUERY END:

delimiter //

-- QUERY START:
create PROCEDURE TigGetComponentVersion(_component varchar(100) CHARSET utf8)
  begin
    select version from tig_schema_versions where (component = _component);
  end //
-- QUERY END:

-- QUERY START:
create procedure TigSetComponentVersion(_component varchar(255) CHARSET utf8, _version mediumtext CHARSET utf8)
  begin
    INSERT INTO tig_schema_versions (component, version, last_update)
    VALUES (_component, _version, now())
    ON DUPLICATE KEY UPDATE
      version = _version, last_update = now();
  end //
-- QUERY END:
