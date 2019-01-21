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


mydir=$1

if [ "$mydir" == "" ] ; then
  echo "Directory name is missing..."
  exit 1
fi

for i in $mydir/* ; do
  newuser=`basename $i`
  echo "Adding new user: "$newuser
  /usr/sbin/useradd $newuser -m
  rm -f $i
  /usr/bin/mail -s "Welcome to Tigase LiveCD" -t ${newuser}@livecd.tigase.org <<EOFMAIL
Hi,

This is an automated message sent to every new user registered on the LiveCD.

Please enjoy the system and send me your comments.

Artur Hefczyc
.
EOFMAIL
done
