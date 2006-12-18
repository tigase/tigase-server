#!/bin/bash

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"
D="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djdbc.drivers=com.mysql.jdbc.Driver"

java $D -cp $CP tigase.util.RepositoryUtils $*
