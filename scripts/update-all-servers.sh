#!/bin/bash

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

	echo -e "\n\n===\tuploading to ${s} ==="

	scp jars/tigase-server.jar tigase@${s}:${DIR}/jars/
	scp libs/tigase-* tigase@${s}:${DIR}/libs/
	scp src/main/groovy/tigase/admin/*.groovy tigase@${s}:${DIR}/scripts/admin/

	echo -e "===\tupload to ${s} DONE ==="

done
