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

  if [[ ${s} != "#"* ]] ; then

	echo -e "\n\n===\trestarting ${s} ==="

	ssh root@${s} "service tigase stop"

	echo -e "===\trestart of ${s} FINISHED ==="

  fi

done

