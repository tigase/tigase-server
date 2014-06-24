#!/bin/bash


if [ "$1" == "" ] ; then
  SERVERS=`cat all-production-servers.txt`
else
  SERVERS=`cat $1`
fi

[[ -f script.conf ]] && . script.conf


TIGASE_USER="tigase"
DIR="/home/${TIGASE_USER}/tigase-server"
PROP_FILE="cluster.properties"
CONF_FILE="cluster.tigase.conf"
JARS="target/tigase-server.jar"
LOG_TIMESTAMP=`date +"%Y-%m-%d_%H-%M-%S"`
IPS=""

echo -e "=== SERVERS:\n${SERVERS}"
echo -e "=== DIR:\n${DIR}"

read -p "Press [Enter] key to start restart..."

function restart_server() {
  if [[ ${s} != "#"* ]] ; then
    if [ -f ${PROP_FILE} ] ; then
      echo -e "\n\n Copying ${PROP_FILE} file to ${s}"
      scp ${PROP_FILE} root@${s}:${DIR}/etc/init.properties
    fi
    if [ -f ${CONF_FILE} ] ; then
      echo -e "\n\n Copying ${CONF_FILE} file to ${s}"
      s_ip=`host ${s} | sed -e "s/.*has address \(.*\)/\1/"`
      echo "The cluster node ${s} IP is: ${s_ip}"
      IPS="${IPS} ${s_ip}"
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
  fi
}


for s in ${SERVERS} ; do

  if [[ ${s} != "#"* ]] ; then

    if [ -f ${PROP_FILE} ] ; then
			echo -e "\n\n Copying ${PROP_FILE} file to ${s}"

      scp ${PROP_FILE} root@${s}:${DIR}/etc/init.properties
    fi

    if [ -f ${CONF_FILE} ] ; then
			echo -e "\n\n Copying ${CONF_FILE} file to ${s}"

			s_ip=`host ${s} | sed -e "s/.*has address \(.*\)/\1/"`

			echo "The cluster node ${s} IP is: ${s_ip}"
			IPS="${IPS} ${s_ip}"

			sed -e "s/\(Djava.rmi.server.hostname=.*\"\)/Djava.rmi.server.hostname=${s_ip}\"/" ${CONF_FILE} > ${CONF_FILE}_${s}

			scp ${CONF_FILE}_${s} root@${s}:${DIR}/etc/tigase.conf
    fi

    echo "Copying jar files"

    for f in ${JARS} ; do

      echo -e "\n\n Copying ${f} file to ${s}"
      [[ -f ${f} ]]  && scp ${f} root@${s}:${DIR}/jars/

    done

	  echo "Checking the system....."
		ssh root@${s} "${DIR}/scripts/machine-check.sh ${s} ${TIGASE_USER} ${VHOST} ; chown -R ${TIGASE_USER}:${TIGASE_USER} /home/${TIGASE_USER}"

		echo -e "\n\n===\trestarting ${s} ==="

    ssh root@${s} "service tigase stop ; sleep 10 ; cp -r ${DIR}/logs ${DIR}/logs_${LOG_TIMESTAMP} ; rm -f ${DIR}/logs/* ; service tigase start"

	  echo -e "===\trestart of ${s} FINISHED ==="

  fi

done

echo -e "nodes="
for i in ${IPS} ; do
  echo -e "${i},"
done
