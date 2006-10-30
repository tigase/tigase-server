#!/bin/bash

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"

java -cp $CP tigase.conf.Configurator $*
