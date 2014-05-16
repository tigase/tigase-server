#!/bin/bash
##
##  Tigase Jabber/XMPP Server
##  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
##
##  This program is free software: you can redistribute it and/or modify
##  it under the terms of the GNU Affero General Public License as published by
##  the Free Software Foundation, either version 3 of the License.
##
##  This program is distributed in the hope that it will be useful,
##  but WITHOUT ANY WARRANTY; without even the implied warranty of
##  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##  GNU Affero General Public License for more details.
##
##  You should have received a copy of the GNU Affero General Public License
##  along with this program. Look for COPYING file in the top folder.
##  If not, see http://www.gnu.org/licenses/.
##
##  $Rev: $
##  Last modified by $Author: $
##  $Date: $
##
#
# This is Tigase (http://www.tigase.org) server startup file.
#
# First parameter is a command (start, stop and so on...),
# second parameters is parameters file - the file where from
# environment variables are read like:
# JAVA_HOME
# JAVA_OPTIONS
# CLASSPATH
# TIGASE_HOME
# TIGASE_CONSOLE_LOG
# TIGASE_PID
# OSGI=true
#
# If not given the script will try to search for the file and if
# not found default parameters will be used.

function usage()
{
  echo "Usage: $0 {start|stop|run|clear|clearrestart|restart|check|status} [params-file.conf]"
  exit 1
}

if [ -z "${2}" ] ; then
  DEF_PARAMS="tigase.conf"
  # Gentoo style config location
  if [ -f "/etc/conf.d/${DEF_PARAMS}" ] ; then
		TIGASE_PARAMS="/etc/conf.d/${DEF_PARAMS}"
  elif [ -f "/etc/${DEF_PARAMS}" ] ; then
		TIGASE_PARAMS="/etc/${DEF_PARAMS}"
  elif [ -f "/etc/tigase/${DEF_PARAMS}" ] ; then
		TIGASE_PARAMS="/etc/tigase/${DEF_PARAMS}"
  else
		TIGASE_PARAMS=""
  fi
  echo "No params-file.conf given. Using: '$TIGASE_PARAMS'"
else
  TIGASE_PARAMS=${2}
fi

[[ -f "${TIGASE_PARAMS}" ]] && . ${TIGASE_PARAMS}

if [ -z "${JAVA_HOME}" ] ; then
  echo "JAVA_HOME is not set."
  echo "Please set it to correct value before starting the sever."
  exit 1
fi

if [ -z "${TIGASE_HOME}" ] ; then
  if [ ${0:0:1} = '/' ] ; then
    TIGASE_HOME=${0}
  else
    TIGASE_HOME=${PWD}/${0}
  fi
  TIGASE_HOME=`dirname ${TIGASE_HOME}`
  TIGASE_HOME=`dirname ${TIGASE_HOME}`

  TIGASE_JAR=""
fi

if [ -n "${OSGI}" ] && ${OSGI} ; then
	LIB_DIR=jars
	JAR_FILE=${LIB_DIR}/org.apache.felix.main*.jar
else
	LIB_DIR=jars
	JAR_FILE=${LIB_DIR}/tigase-server*.jar
fi

for j in ${TIGASE_HOME}/${JAR_FILE} ; do
	if [ -f ${j} ] ; then
	  TIGASE_JAR=${j}
	  break
	fi
done

if [ -z ${TIGASE_JAR} ] ; then
	echo "TIGASE_HOME is not set."
	echo "Please set it to correct value before starting the sever."
	exit 1
fi

if [ -z "${TIGASE_CONSOLE_LOG}" ] ; then
  if [ ! -d "logs" ] ; then
    mkdir logs
  fi
  if [ -w "${TIGASE_HOME}/logs/" ] ; then
		TIGASE_CONSOLE_LOG="${TIGASE_HOME}/logs/tigase-console.log"
  else
		TIGASE_CONSOLE_LOG="/dev/null"
  fi
fi

if [ -z "${TIGASE_PID}" ] ; then
  if [ -w "${TIGASE_HOME}/logs/" ] ; then
		TIGASE_PID="${TIGASE_HOME}/logs/tigase.pid"
  else
		if [ -w "/var/run/" ] ; then
	    TIGASE_PID="/var/run/tigase.pid"
		else
	    TIGASE_PID="/var/tmp/tigase.pid"
		fi
  fi
fi

[[ -z "${TIGASE_RUN}" ]] && \
  TIGASE_RUN="tigase.server.XMPPServer ${TIGASE_OPTIONS}"

[[ -z "${JAVA}" ]] && JAVA="${JAVA_HOME}/bin/java"

[[ -z "${CLASSPATH}" ]] || CLASSPATH="${CLASSPATH}:"

CLASSPATH="${CLASSPATH}${TIGASE_JAR}"

CLASSPATH="`ls -d ${TIGASE_HOME}/${LIB_DIR}/*.jar 2>/dev/null | grep -v wrapper | tr '\n' :`${CLASSPATH}"

LOGBACK="-Dlogback.configurationFile=$TIGASE_HOME/etc/logback.xml"

if [ -n "${OSGI}" ] && ${OSGI} ; then
	TIGASE_CMD="${JAVA} ${JAVA_OPTIONS} ${LOGBACK} -jar ${JAR_FILE}"
else
	TIGASE_CMD="${JAVA} ${JAVA_OPTIONS} ${LOGBACK} -cp ${CLASSPATH} ${TIGASE_RUN}"
fi

cd "${TIGASE_HOME}"

case "${1}" in
  start)
    echo "Starting Tigase: "

    if [ -f ${TIGASE_PID} ] && kill -0 $(<${TIGASE_PID}) 2>/dev/null
    then
      echo "Already Running!!"
      exit 1
    fi

    echo "STARTED Tigase `date`" >> ${TIGASE_CONSOLE_LOG}

    nohup sh -c "exec $TIGASE_CMD >>${TIGASE_CONSOLE_LOG} 2>&1" >/dev/null &
    echo $! > $TIGASE_PID
    echo "Tigase running pid="`cat $TIGASE_PID`
    ;;

  stop)
    PID=`cat $TIGASE_PID 2>/dev/null`
    if [ -z "$PID" ] ; then
      echo "Tigase is not running."
      exit 0
    fi
    echo "Shutting down Tigase: $PID"

	kill $PID 2>/dev/null
	for ((i=1; i <= 10; i++)) ; do
	  if ps -p $PID > /dev/null ; then
		echo "$i. Waiting for the server to terminate..."
		sleep 1
	  else
		echo "$i. Tigase terminated."
		break
	  fi
	done
	
	if ps -p $PID > /dev/null ; then
      echo "Forcing the server to terminate."
      kill -9 $PID 2>/dev/null
    fi
    rm -f $TIGASE_PID
    echo "STOPPED `date`" >>${TIGASE_CONSOLE_LOG}
    ;;

  restart)
    $0 stop $2
    sleep 5
    $0 start $2
    ;;

  clearrestart)
    $0 stop $2
    sleep 5
    $0 clear $2
    sleep 2
    $0 start $2
    ;;

  clear)
    LOGBACK="${TIGASE_HOME}/logs"`date "+%Y-%m-%d--%H:%M:%S"`
	echo "Clearing logs, moving backup to " ${LOGBACK}
	mkdir -p ${LOGBACK}
	mv "${TIGASE_HOME}/logs"/* ${LOGBACK}/
	if [ -n "${OSGI}" ] && ${OSGI} ; then
		echo "Clearing osgi cache"
		rm -rf "${TIGASE_HOME}/felix-cache"/*;
	fi
    ;;

  run)
    echo "Running Tigase: "

    if [ -f $TIGASE_PID ]
    then
      echo "Already Running!!"
      exit 1
    fi

    sh -c "exec $TIGASE_CMD"
    ;;

  check|status)
    echo "Checking arguments to Tigase: "
    echo "OSGI            =  $OSGI"
    echo "TIGASE_HOME     =  $TIGASE_HOME"
    echo "TIGASE_JAR      =  $TIGASE_JAR"
    echo "TIGASE_PARAMS   =  $TIGASE_PARAMS"
    echo "TIGASE_RUN      =  $TIGASE_RUN"
    echo "TIGASE_PID      =  $TIGASE_PID"
    echo "TIGASE_OPTIONS  =  $TIGASE_OPTIONS"
    echo "JAVA_OPTIONS    =  $JAVA_OPTIONS"
    echo "JAVA            =  $JAVA"
    echo "JAVA_CMD        =  $JAVA_CMD"
    echo "CLASSPATH       =  $CLASSPATH"
    echo "TIGASE_CMD      =  $TIGASE_CMD"
    echo "TIGASE_CONSOLE_LOG  =  $TIGASE_CONSOLE_LOG"
    echo

    if [ -f ${TIGASE_PID} ] && kill -0 $(<${TIGASE_PID}) 2>/dev/null
    then
      echo "Tigase running pid="`cat ${TIGASE_PID}`
      exit 0
    fi
    exit 1
    ;;
  zap)
		rm -f $TIGASE_PID
		;;

	*)
    usage
		;;
esac
