#!/bin/bash
##
##  Tigase Jabber/XMPP Server
##  Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
##
##  This program is free software: you can redistribute it and/or modify
##  it under the terms of the GNU General Public License as published by
##  the Free Software Foundation, either version 3 of the License.
##
##  This program is distributed in the hope that it will be useful,
##  but WITHOUT ANY WARRANTY; without even the implied warranty of
##  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##  GNU General Public License for more details.
##
##  You should have received a copy of the GNU General Public License
##  along with this program. Look for COPYING file in the top folder.
##  If not, see http://www.gnu.org/licenses/.
##
##  $Rev: $
##  Last modified by $Author: $
##  $Date: $
##

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/pg73jdbc3.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"

D="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djdbc.drivers=com.mysql.jdbc.Driver:org.postgresql.Driver"

XML_REP="-sc tigase.db.xml.XMLRepository -su user-repository.xml"
MYSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:mysql://localhost/tigase?user=root&password=mypass"
PGSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:postgresql://localhost/tigase?user=tigase"

java $D -cp $CP tigase.util.RepositoryUtils $*