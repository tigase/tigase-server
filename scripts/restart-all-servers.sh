#!/bin/bash


if [ "$1" == "" ] ; then
  SERVERS=`cat all-production-servers.txt`
else
  SERVERS=`cat $1`
fi


TIGASE_USER="tigase"
DIR="/home/${TIGASE_USER}/tigase-server"
PROP_FILE="cluster.properties"
JARS="target/tigase-server.jar"
LOG_TIMESTAMP=`date +"%Y-%m-%d_%H-%M-%S"`

echo -e "=== SERVERS:\n${SERVERS}"
echo -e "=== DIR:\n${DIR}"

read -p "Press [Enter] key to start restart..."

for s in ${SERVERS} ; do

  if [[ ${s} != "#"* ]] ; then

    echo -e "\n\n Copying ${PROP_FILE} file to ${s}"

    [[ -f ${PROP_FILE} ]]  && scp ${PROP_FILE} root@${s}:${DIR}/etc/init.properties

    echo "Copying jar files"

    for f in ${JARS} ; do

      echo -e "\n\n Copying ${f} file to ${s}"
      [[ -f ${f} ]]  && scp ${f} root@${s}:${DIR}/jars/

    done

	  echo -e "\n\n===\trestarting ${s} ==="

    ssh root@${s} "chown -R ${TIGASE_USER}:${TIGASE_USER} /home/${TIGASE_USER} ; service tigase stop ; sleep 10 ; cp -r ${DIR}/logs ${DIR}/logs_${LOG_TIMESTAMP} ; rm -f ${DIR}/logs/* ; service tigase start"

	  echo -e "===\trestart of ${s} FINISHED ==="

  fi

done
