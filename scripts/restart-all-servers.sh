#!/bin/bash


if [ "$1" == "" ] ; then
  SERVERS=`cat all-production-servers.txt`
else
  SERVERS=`cat $1`
fi
DIR="tigase-server"

for s in ${SERVERS} ; do 

  if [[ ${s} != "#"* ]] ; then 
    ssh root@${s} "/etc/init.d/tigase stop ; sleep 10 ; rm -f /home/tigase/${DIR}/logs/* ;  /etc/init.d/tigase start"
  fi

done
