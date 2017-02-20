#!/bin/bash

echo -e "\n\nusage: db-create-mysql.sh tigase_username tigase_password database_name database_host \n\n"
 
if [ "${1}" = "-y" ] ; then
  NONINTERACTIVE=yes
  shift
fi

if [ -z "${1}" ] ; then
  echo "No username given. Using: tigase_user"
  USER_NAME=tigase_user
else
  USER_NAME="${1}"
fi

if [ -z "${2}" ] ; then
  echo "No password given. Using: tigase_passwd"
  USER_PASS=tigase_passwd
else
  USER_PASS="${2}"
fi

if [ -z "${3}" ] ; then
  echo "No DB name given. Using: tigasedb"
  DB_NAME=tigasedb
else
  DB_NAME="${3}"
fi


if [ -z "${4}" ] ; then
  echo "No DB hostname given. Using: localhost"
  DB_HOST=localhost
else
  DB_HOST="${4}"
fi


if [ -z "$NONINTERACTIVE" ] ; then
  echo ""
  echo "creating ${DB_NAME} database for user ${USER_NAME} identified by ${USR_PASS} password:"
  echo ""
 
  read -p "Press [Enter] key to start, otherwise abort..."
else
  echo "User: $USER_NAME, Pass: $USR_PASS, Db: $DB_NAME, Host: $DB_HOST"
fi


export DB_TYPE=postgresql
export DB_VERSION=7-1

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL -file database/${DB_TYPE}-pubsub-schema-3.2.0.sql

echo -e "\n\n\nconfiguration:\n\n--user-db=pgsql\n--user-db-uri=jdbc:postgresql://$DB_HOST/$DB_NAME?user=$USR_NAME&password=$USR_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"
