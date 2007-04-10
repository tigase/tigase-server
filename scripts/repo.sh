#!/bin/bash

CP="jars/tigase-server.jar:/usr/share/jdbc-mysql/lib/jdbc-mysql.jar:libs/pg73jdbc3.jar:libs/tigase-xmltools.jar:libs/tigase-utils.jar"

D="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djdbc.drivers=com.mysql.jdbc.Driver:org.postgresql.Driver"

XML_REP="-sc tigase.db.xml.XMLRepository -su user-repository.xml"
MYSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:mysql://localhost/tigase?user=root&password=mypass"
PGSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:postgresql://localhost/tigase?user=tigase"

java $D -cp $CP tigase.util.RepositoryUtils $*
