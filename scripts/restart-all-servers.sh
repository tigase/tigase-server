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
  SERVS=`cat all-production-servers.txt`
else
  SERVS=`cat $1`
fi

[[ -f script.conf ]] && . script.conf

SERVERS=""

for s in "${SERVS}" ; do
  if [[ ${s} != "#"* ]] ; then
    SERVERS="${SERVERS} ${s}"
  fi
done

TIGASE_USER="tigase"
DIR="/home/${TIGASE_USER}/tigase-server"
PROP_FILE="cluster.properties"
CONF_FILE="cluster.tigase.conf"
JARS="target/tigase-server.jar"
LOG_TIMESTAMP=`date +"%Y-%m-%d_%H-%M-%S"`
IPS=()
COLORS=('green' 'yellow' 'orange' 'blue' 'white' 'lightblue' 'gray' 'pink' 'lightgreen' 'red')

echo -e "=== SERVERS:\n${SERVERS}"
echo -e "=== DIR:\n${DIR}"

read -p "Press [Enter] key to start restart..."

function restart_server() {

  s=$1
  s_ip=$2
  echo "Restarting: ${s} ${s_ip}"

  if [ -f ${PROP_FILE} ] ; then
    echo -e "\n\n Copying ${PROP_FILE} file to ${s}"
    scp ${PROP_FILE} root@${s}:${DIR}/etc/init.properties
  fi
  if [ -f ${CONF_FILE} ] ; then
    echo -e "\n\n Copying ${CONF_FILE} file to ${s}"
    echo "The cluster node ${s} IP is: ${s_ip}"
    sed -e "s/\(Djava.rmi.server.hostname=.*\"\)/Djava.rmi.server.hostname=${s_ip}\"/" ${CONF_FILE} > ${CONF_FILE}_${s}
    scp ${CONF_FILE}_${s} root@${s}:${DIR}/etc/tigase.conf
  fi
  echo "Copying jar files"
  for f in ${JARS} ; do
    echo -e "\n\n Copying ${f} file to ${s}"
    [[ -f ${f} ]]  && scp ${f} root@${s}:${DIR}/jars/
  done
  echo -e "\n\n===\trestarting ${s} ==="
  ssh root@${s} "chown -R ${TIGASE_USER}:${TIGASE_USER} /home/${TIGASE_USER} ; service tigase stop ; sleep 10 ; cp -r $\
{DIR}/logs ${DIR}/logs_${LOG_TIMESTAMP} ; rm -f ${DIR}/logs/* ; service tigase start"
  echo -e "===\trestart of ${s} FINISHED ==="
}

function restart_tmp() {

  s=$1
  s_ip=$2
	echo "Restarting: ${s} ${s_ip}"
  sleep 5
	echo "${s} restarted"

}

echo "Resolving IPs..."

cnt=0
for s in ${SERVERS} ; do

  IPS[$cnt]=`host ${s} | sed -e "s/.*has address \(.*\)/\1/"`
  echo "${COLORS[$cnt]}:${s}:${IPS[$cnt]}"
  ((cnt++))

done

echo -ne "nodes="
COLORS=('green' 'yellow' 'orange' 'blue' 'white' 'lightblue' 'gray' 'pink' 'lightgreen' 'red')
for i in "${!IPS[@]}"; do
  echo -ne "${COLORS[$i]}:${IPS[$i]},"
done
echo ""

cnt=0
for s in ${SERVERS} ; do

  s_ip=${IPS[$cnt]}
  ((cnt++))

  restart_server $s $s_ip&

done
