#!/bin/bash
#
# Tigase XMPP Server - The instant messaging server
# Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. Look for COPYING file in the top folder.
# If not, see http://www.gnu.org/licenses/.
#

LIBS="/usr/share/tigase/lib /usr/share/openfire/lib"

# Locale check
if ! locale | grep -iq '^LC_CTYPE=.*\.utf8.\?$'; then
	echo "You need to set UTF8 based locale first" >&2; exit 1
fi

for DIR in $LIBS; do CLASSPATH="`ls -d $DIR/*.jar 2>/dev/null | grep -v groovy | tr '\n' :`$CLASSPATH"; done
export CLASSPATH
#echo $CLASSPATH
exec ${0/%.sh/.groovy}
