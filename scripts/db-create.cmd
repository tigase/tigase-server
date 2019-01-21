@REM
@REM Tigase XMPP Server - The instant messaging server
@REM Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
@REM
@REM This program is free software: you can redistribute it and/or modify
@REM it under the terms of the GNU Affero General Public License as published by
@REM the Free Software Foundation, version 3 of the License.
@REM
@REM This program is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@REM GNU Affero General Public License for more details.
@REM
@REM You should have received a copy of the GNU Affero General Public License
@REM along with this program. Look for COPYING file in the top folder.
@REM If not, see http://www.gnu.org/licenses/.
@REM

@echo off

java -cp "jars/*" tigase.db.util.DBSchemaLoader %*