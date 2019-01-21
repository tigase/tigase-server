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


MY_DIR=`dirname ${0}`

# Load external script with some utility functions
. ${MY_DIR}/update-code-functions.sh

# Load configuration
# PROJECTS_DIR - directory with source codes for all projects:
# xmltools, utils, extras, server, muc, pubsub, socks5, stun, archiving
# TARGET_DIR - a directory where the installed serve binaries are located

CONFIG="${MY_DIR}/update-code-config.sh"
if [ "$1" != "" ] ; then
  CONFIG="$1"
fi
. ${CONFIG}

SRV_PACKAGES="xmltools utils extras server muc pubsub archiving socks5 stun tigase_http_api"

CUR_DIR=`pwd`

cd ${PROJECTS_DIR}

#set -x

echo "Updating and building packages..."

# Build all server packages
for p in $SRV_PACKAGES ; do
  echo "Building package: $p"
  cd $p
  mkdir -p jars
  git pull
  JAR=`build_$p`
  echo "jar file: $JAR"
  if [ ! -z "${JAR}" ] ; then
    cp -f $JAR ../server/jars/tigase-$p.jar
  fi
  cd ..
done


cd ${CUR_DIR}

if [ "${TARGET_DIR}" != "" ] ; then
  cp -fv ${PROJECTS_DIR}/server/jars/* ${TARGET_DIR}/bundle/
	cp -fv ${PROJECTS_DIR}/server/jars/* ${TARGET_DIR}/jars/
fi
