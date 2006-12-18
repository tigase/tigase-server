#!/bin/bash

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"

java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -cp $CP tigase.conf.Configurator $*
