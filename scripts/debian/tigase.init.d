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
PATH=/sbin:/bin:/usr/sbin:/usr/bin:${JAVA_HOME}/bin
JAVA=${JAVA_HOME}/bin/java

TIGASE_HOME=/usr/share/tigase
TIGASE_LIB=${TIGASE_HOME}/libs
TIGASE_CONFIG=/etc/tigase/tigase-server.xml
TIGASE_OPTIONS=

USERNAME=tigase
NAME=tigase
DESC="Tigase XMPP server"

# Include tigase defaults if available
if [ -f "/etc/default/tigase" ] ; then
	TIGASE_PARAMS="/etc/default/tigase"
elif [ -f "/etc/tigase/tigase.conf" ] ; then
	TIGASE_PARAMS="/etc/tigase/tigase.conf"
elif [ -f "${TIGASE_HOME}/etc/tigase.conf" ] ; then
	TIGASE_PARAMS="${TIGASE_HOME}/etc/tigase.conf"
fi

[[ -f "${TIGASE_PARAMS}" ]] && . ${TIGASE_PARAMS}

if [ -z "${JAVA_HOME}" ] ; then
  echo "JAVA_HOME is not set."
  echo "Please set it to correct value before starting the sever."
  exit 1
fi

# Find tigase-server jar
for j in ${TIGASE_HOME}/jars/tigase-server*.jar ; do
        if [ -f ${j} ] ; then
          TIGASE_JAR=${j}
          break
        fi
done

[[ -z "${TIGASE_RUN}" ]] && \
  TIGASE_RUN="tigase.server.XMPPServer -c ${TIGASE_CONFIG} ${TIGASE_OPTIONS}"

[[ -z "${JAVA}" ]] && JAVA="${JAVA_HOME}/bin/java"

[[ -z "${CLASSPATH}" ]] || CLASSPATH="${CLASSPATH}:"

CLASSPATH="${CLASSPATH}${TIGASE_JAR}"

for lib in ${TIGASE_LIB}/*.jar ; do
  CLASSPATH="${CLASSPATH}:$lib"
done

TIGASE_CMD="${JAVA_OPTIONS} -cp ${CLASSPATH} ${TIGASE_RUN}"

PIDFILE="/var/run/$NAME.pid"

cd ${TIGASE_HOME}

set -e

. /lib/lsb/init-functions

#Helper functions
start() {
        start-stop-daemon --start --background --quiet --make-pidfile \
                --chdir ${TIGASE_HOME} --pidfile $PIDFILE --chuid $USERNAME:$USERNAME \
                --exec $JAVA -- $TIGASE_CMD
}

stop() {
        start-stop-daemon --stop --quiet \
        	--chdir ${TIGASE_HOME} --pidfile $PIDFILE --chuid $USERNAME:$USERNAME \
		--exec $JAVA  > /dev/null
		
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
	echo "PIDFILE             =  $PIDFILE"
	echo "JAVA_OPTIONS        =  $JAVA_OPTIONS"
	echo "JAVA_HOME           =  $JAVA_HOME"
	echo "JAVA                =  $JAVA"
	echo "USERNAME            =  $USERNAME"
	echo "TIGASE_CMD          =  $TIGASE_CMD"
	echo "TIGASE_HOME         =  $TIGASE_HOME"
	echo "TIGASE_JAR          =  $TIGASE_JAR"
	echo "TIGASE_CONFIG       =  $TIGASE_CONFIG"
	echo "TIGASE_OPTIONS      =  $TIGASE_OPTIONS"
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
