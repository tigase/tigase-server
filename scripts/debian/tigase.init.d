#!/bin/bash
### BEGIN INIT INFO
# Provides:          tigase
# Required-Start:    networking
# Required-Stop:     networking
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start the Tigase XMPP server
### END INIT INFO

# Attempt to locate JAVA_HOME, code borrowed from jabref package
if [ -z $JAVA_HOME ]
then
	for java_dir in /usr/lib/jvm/java-6-* ; do
		test -d ${java_dir} && JAVA_HOME=${java_dir}
	done
fi

# Settings paths and other variables
USERNAME=tigase
USERGROUP=tigase
NAME=tigase
DESC="Tigase XMPP server"

TIGASE_HOME=/usr/share/tigase
TIGASE_LIB=${TIGASE_HOME}/libs
TIGASE_CONFIG=/etc/tigase/tigase-server.xml
TIGASE_OPTIONS=
TIGASE_PARAMS=

PIDFILE=
TIGASE_CONSOLE_LOG=

USER=$USERNAME
eval HOME="~$USER"

# Include tigase defaults if available
if [ -z "${TIGASE_PARAMS}" ] ; then
	if [ -r "/etc/default/tigase" ] ; then
		TIGASE_PARAMS="/etc/default/tigase"
	elif [ -r "/etc/tigase/tigase.conf" ] ; then
		TIGASE_PARAMS="/etc/tigase/tigase.conf"
	elif [ -r "${TIGASE_HOME}/etc/tigase.conf" ] ; then
		TIGASE_PARAMS="${TIGASE_HOME}/etc/tigase.conf"
	fi
fi

[[ -f "${TIGASE_PARAMS}" ]] && . ${TIGASE_PARAMS}

if [ -z "${JAVA_HOME}" ] ; then
  echo "JAVA_HOME is not set."
  echo "Please set it to correct value before starting the sever."
  exit 1
fi

PATH=/sbin:/bin:/usr/sbin:/usr/bin:${JAVA_HOME}/bin

# Find tigase-server jar
for j in ${TIGASE_HOME}/jars/tigase-server*.jar ; do
        if [ -f ${j} ] ; then
          TIGASE_JAR=${j}
          break
        fi
done

if [ -z "${TIGASE_CONSOLE_LOG}" ] ; then
    if [ -w "${TIGASE_HOME}/logs/" ] ; then
        TIGASE_CONSOLE_LOG="${TIGASE_HOME}/logs/tigase-console.log"
    elif [ -w "/var/log/${NAME}/" ] ; then
        TIGASE_CONSOLE_LOG="/var/log/${NAME}/tigase-console.log"
    else
        TIGASE_CONSOLE_LOG="/dev/null"
    fi
fi

if [ -z "${PIDFILE}" ] ; then
    if [ -w "${TIGASE_HOME}/logs/" ] ; then
        PIDFILE="${TIGASE_HOME}/logs/$NAME.pid"
    elif [ -w "/var/run/" ] ; then
        PIDFILE="/var/run/$NAME.pid"
    else
        PIDFILE="/var/tmp/$NAME.pid"
    fi
fi

[[ -z "${TIGASE_RUN}" ]] && \
  TIGASE_RUN="tigase.server.XMPPServer -c ${TIGASE_CONFIG} ${TIGASE_OPTIONS}"

[[ -z "${JAVA}" ]] && JAVA="${JAVA_HOME}/bin/java"

[[ -z "${CLASSPATH}" ]] || CLASSPATH="${CLASSPATH}:"

CLASSPATH="${CLASSPATH}${TIGASE_JAR}"

for lib in ${TIGASE_LIB}/*.jar ; do
  CLASSPATH="${CLASSPATH}:$lib"
done

TIGASE_CMD="${JAVA_OPTIONS} -cp ${CLASSPATH} ${TIGASE_RUN}"

if [ -d "${TIGASE_HOME}" ] ; then
        cd ${TIGASE_HOME}
else
  echo "${TIGASE_HOME} is not set to correct value"
  echo "Please set it to correct value before starting the sever."
  exit 1
fi

set -e

. /lib/lsb/init-functions

#Helper functions
start() {

	if [ -f $PIDFILE ] && kill -0 `cat $PIDFILE` 2>/dev/null
        then
            echo "Tigase is already running!"
            return 1
        fi

	su ${USERNAME} -c "start-stop-daemon --start --quiet --make-pidfile --chdir ${TIGASE_HOME} --pidfile $PIDFILE --chuid $USERNAME:$USERGROUP --exec $JAVA -- $TIGASE_CMD >>${TIGASE_CONSOLE_LOG} 2>&1 &"

	sleep 3
	PID=`cat $PIDFILE`

	if [[ -z "`ps -p ${PID} -o cmd=`" ]]; then
		rm -f "$PIDFILE"
		return 1
	else
		return 0
	fi


}

stop() {
        su ${USERNAME} -c "start-stop-daemon --stop --quiet  --chdir ${TIGASE_HOME} --pidfile $PIDFILE --chuid $USERNAME:$USERGROUP  --exec $JAVA > /dev/null"
        
	rm -f "$PIDFILE"
}

case "$1" in
  start)
	log_daemon_msg "Starting $DESC"
	if start; then
		log_end_msg 0
	else
		log_end_msg 1
	fi
	;;
  stop)
	log_daemon_msg "Stopping $DESC"
	if stop; then
		log_end_msg 0
	else
		log_end_msg 1
	fi
	;;
  restart|force-reload)
	log_daemon_msg "Restarting $DESC"
	stop
	sleep 1
	if start; then
		log_end_msg 0
	else
		log_end_msg 1
	fi
	;;

  check)
	echo "Checking arguments to Tigase: "
	echo
	echo "USERNAME            =  $USERNAME"
	echo "USERGROUP           =  $USERGROUP"
	echo "USER                =  $USER"
	echo "HOME                =  $HOME"
	echo
	echo "TIGASE_HOME         =  $TIGASE_HOME"
	echo "TIGASE_JAR          =  $TIGASE_JAR"
	echo "TIGASE_CMD          =  $TIGASE_CMD"
	echo "TIGASE_CONFIG       =  $TIGASE_CONFIG"
	echo "TIGASE_PARAMS       =  $TIGASE_PARAMS"
	echo "TIGASE_OPTIONS      =  $TIGASE_OPTIONS"
	echo "TIGASE_CONSOLE_LOG  =  $TIGASE_CONSOLE_LOG"
	echo "PIDFILE             =  $PIDFILE"
	echo "JAVA_HOME           =  $JAVA_HOME"
	echo "JAVA                =  $JAVA"
	echo "JAVA_OPTIONS        =  $JAVA_OPTIONS"
	echo "CLASSPATH           =  $CLASSPATH"

	if [ -f $PIDFILE ] && kill -0 `cat $PIDFILE` 2>/dev/null
	then
		echo "Tigase running pid="`cat $PIDFILE`
		exit 0
	fi
	exit 1
	;;

  *)
	N=/etc/init.d/$NAME
	echo "Usage: $N {start|stop|restart|force-reload|check}" >&2
	exit 1
	;;
esac

exit 0
