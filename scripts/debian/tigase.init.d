#!/bin/bash
### BEGIN INIT INFO
# Provides:          tigase
# Required-Start:    networking
# Required-Stop:     networking
# Default-Start:     2 3 5
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

USERNAME=tigase
PATH=/sbin:/bin:/usr/sbin:/usr/bin:${JAVA_HOME}/bin
JAVA=${JAVA_HOME}/bin/java
NAME=tigase
DESC="Tigase XMPP server"
TIGASE_HOME=/usr/share/tigase
TIGASE_LIB=${TIGASE_HOME}/libs
TIGASE_CONFIG=/etc/tigase/tigase-server.xml
TIGASE_OPTIONS=

# Include tigase defaults if available
if [ -f /etc/default/tigase ] ; then
	. /etc/default/tigase
fi

if [ -z "${JAVA_HOME}" ] ; then
  echo "JAVA_HOME is not set."
  echo "Please set it to correct value before starting the sever."
  exit 1
fi

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

for lib in ${TIGASE_LIB}/* ; do
  CLASSPATH="${CLASSPATH}:$lib"
done

TIGASE_CMD="${JAVA_OPTIONS} -cp ${CLASSPATH} ${TIGASE_RUN}"

cd ${TIGASE_HOME}

set -e

. /lib/lsb/init-functions

#Helper functions
start() {
        start-stop-daemon --start --quiet --background --make-pidfile \
                --chdir ${TIGASE_HOME} --pidfile /var/run/$NAME.pid --chuid $USERNAME:$USERNAME \
                --exec $JAVA -- $TIGASE_CMD
}

stop() {
        start-stop-daemon --stop --quiet --pidfile /var/run/$NAME.pid \
		--exec $JAVA --retry 4
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
	stop
	log_end_msg 0
	#else
	#	log_end_msg 1
	#fi
	;;
  restart|force-reload)
	#
	#	If the "reload" option is implemented, move the "force-reload"
	#	option to the "reload" entry above. If not, "force-reload" is
	#	just the same as "restart".
	#
	log_daemon_msg "Restarting $DESC"
	#set +e
	stop
	#set -e
	#sleep 1
	if start; then
		log_end_msg 0
	else
		log_end_msg 1
	fi
	;;
  *)
	N=/etc/init.d/$NAME
	# echo "Usage: $N {start|stop|restart|reload|force-reload}" >&2
	echo "Usage: $N {start|stop|restart|force-reload}" >&2
	exit 1
	;;
esac

exit 0
