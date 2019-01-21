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



if [ "$1" == "" ] ; then
  SERVERS=`cat all-production-servers.txt`
else
  SERVERS=`cat $1`
fi

DIR="/home/tigase/tigase-server"

echo -e "=== SERVERS:\n${SERVERS}"
echo -e "=== DIR:\n${DIR}"

read -p "Press [Enter] key to start restart..."

for s in ${SERVERS} ; do

  if [[ ${s} != "#"* ]] ; then

	echo -e "\n\n===\trestarting ${s} ==="

	ssh root@${s} "service tigase stop"

	echo -e "===\trestart of ${s} FINISHED ==="

  fi

done

