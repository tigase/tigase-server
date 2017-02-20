#!/bin/bash

echo -e "\n\nusage: db-create-mysql.sh tigase_username tigase_password database_name root_username root_password database_host \n\n"

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
  echo "No DB root username given. Using: root"
  ROOT_NAME=root
else
  ROOT_NAME="${4}"
fi

if [ -z "${5}" ] ; then
  echo "No DB root password given. Using: root"
  ROOT_PASS=root
else
  ROOT_PASS="${5}"
fi

if [ -z "${6}" ] ; then
  echo "No DB hostname given. Using: localhost"
  DB_HOST=localhost
else
  DB_HOST="${6}"
fi


if [ -z "$NONINTERACTIVE" ] ; then
  echo ""
  echo "creating ${DB_NAME} database for user ${USER_NAME} identified by ${USER_PASS} password:"
  echo ""

  read -p "Press [Enter] key to start, otherwise abort..."
else
  echo "User: $USER_NAME, Pass: $USER_PASS, Db: $DB_NAME, Db Admin: $ROOT_NAME, Admin Pass: $ROOT_PASS, Host: $DB_HOST"
fi

export DB_TYPE=mysql
export VERSION=7-1

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL -file database/${DB_TYPE}-pubsub-schema-3.2.0.sql


echo -e "\n\n\nconfiguration:\n\n--user-db=mysql\n--user-db-uri=jdbc:mysql://$DB_HOST:3306/$DB_NAME?user=${USER_NAME}&password=${USER_PASS}&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"
