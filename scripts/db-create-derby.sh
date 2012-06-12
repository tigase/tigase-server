#!/bin/bash

[[ "$1" = "" ]] && \
  echo "Give me a path to the location where you want to have the database created" && \
  exit 1

# for tigase 5.0 and below
#java -Dij.protocol=jdbc:derby: -Dij.database="$1;create=true" \
#		-Dderby.system.home=`pwd` \
#		-cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar \
#		org.apache.derby.tools.ij database/derby-schema-4.sql
#java -Dij.protocol=jdbc:derby: -Dij.database="$1" \
#		-Dderby.system.home=`pwd` \
#		-cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar \
#		org.apache.derby.tools.ij database/derby-schema-4-sp.schema
#java -Dij.protocol=jdbc:derby: -Dij.database="$1" \
#		-Dderby.system.home=`pwd` \
#		-cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar \
#		org.apache.derby.tools.ij database/derby-schema-4-props.sql

# for Tigase 5.1

java -Dij.protocol=jdbc:derby: -Dij.database="$1;create=true" \
		-Dderby.system.home=`pwd` \
		-cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar \
		org.apache.derby.tools.ij database/derby-schema-5.1.sql

echo -e "\n\n\nconfiguration:\n\n--user-db=derby\n--user-db-uri=jdbc:derby:$1\n\n"
