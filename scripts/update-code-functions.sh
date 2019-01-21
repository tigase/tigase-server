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


export TMP_DIR="/tmp"

function build_xmltools() {
   mvn -q clean package install &> ${TMP_DIR}/xmltools-build.txt
   ant clean jar-dist &> ${TMP_DIR}/xmltools-build.txt
   echo jars/tigase-xmltools.jar
 }

 function build_utils() {
   mvn -q clean package install &> ${TMP_DIR}/utils-build.txt
   cp -f ../xmltools/jars/tigase-xmltools.jar jars/
   ant clean jar-dist &> ${TMP_DIR}/utils-build.txt
   echo jars/tigase-utils.jar
 }

 function build_server() {
   mvn -q clean package install &> ${TMP_DIR}/server-build.txt
   ant clean jar-dist &> ${TMP_DIR}/server-build.txt
   echo ""
 }

 function build_maven() {
   mvn clean package &> ${TMP_DIR}/$1-build.txt
   echo `find target -name "tigase-*.jar"`
 }

 function build_extras() {
   build_maven extras
 }

 function build_muc() {
   build_maven muc
 }

 function build_pubsub() {
   build_maven pubsub
 }

function build_archiving() {
	build_maven archiving
}

function build_socks5() {
	build_maven socks5
}

function build_stun() {
	build_maven stun
}

function build_tigase_http_api() {
  build_maven "tigase-http-api"
}
