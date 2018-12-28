#!/bin/bash
##
##  Tigase Jabber/XMPP Server
##  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
##
##  This program is free software: you can redistribute it and/or modify
##  it under the terms of the GNU Affero General Public License as published by
##  the Free Software Foundation, either version 3 of the License.
##
##  This program is distributed in the hope that it will be useful,
##  but WITHOUT ANY WARRANTY; without even the implied warranty of
##  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##  GNU Affero General Public License for more details.
##
##  You should have received a copy of the GNU Affero General Public License
##  along with this program. Look for COPYING file in the top folder.
##  If not, see http://www.gnu.org/licenses/.
##
##  $Rev: $
##  Last modified by $Author: $
##  $Date: $
##

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"

java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -cp $CP tigase.conf.Configurator $*