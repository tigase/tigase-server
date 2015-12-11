#!/bin/bash

[[ "$1" = "" ]] && \
  echo "Give me a path to the location where you want to have the database created" && \
  exit 1

java -Dij.protocol=jdbc:derby: -Dij.database="$1;create=true" \
		-Dderby.system.home=`pwd` \
		-cp jars/derby.jar:jars/derbytools.jar:jars/tigase-server.jar \
		org.apache.derby.tools.ij
