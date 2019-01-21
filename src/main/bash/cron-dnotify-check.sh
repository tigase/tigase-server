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


monitored_dir="/home/webapp/drupal-sites/users-list"
mydir=`dirname $0`

cron_check=`ps axw | grep -c "[d]notify -C"`

if [ $cron_check -eq 0 ] ; then 
#  echo "dnotify monitor is not running, starting a new one...."
  /usr/bin/dnotify -C -b $monitored_dir -e $mydir/users-list-moitor.sh '{}'
fi
