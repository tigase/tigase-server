#!/bin/bash

SERVERS=`cat all-servers.txt`
DIR="tigase-server"

for s in ${SERVERS} ; do 

  if [[ ${s} != "#"* ]] ; then 
    ssh root@${s} "/etc/init.d/tigase stop ; sleep 10 ; rm -f /home/tigase/${DIR}/logs/* ;  /etc/init.d/tigase start"
  fi

done
