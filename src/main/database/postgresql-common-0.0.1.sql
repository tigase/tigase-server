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
CREATE OR REPLACE FUNCTION TigGetComponentVersion(_component VARCHAR(100))
  RETURNS TEXT AS $$
DECLARE _result TEXT;
BEGIN

  SELECT version INTO _result FROM tig_schema_versions WHERE (component = _component);
  RETURN (_result);

END;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
CREATE OR REPLACE FUNCTION TigSetComponentVersion(_component text, _version text)
  RETURNS void AS $$

  BEGIN
      UPDATE tig_schema_versions SET version = _version, last_update = now() WHERE component = _component;
      IF FOUND THEN
        RETURN;
      END IF;
      BEGIN
        INSERT INTO tig_schema_versions (component, version, last_update) VALUES (_component, _version, now());
        EXCEPTION WHEN OTHERS THEN
        UPDATE tig_schema_versions SET version = _version WHERE component = _component;
      END;
      RETURN;

  END;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

