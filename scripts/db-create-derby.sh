#!/bin/bash

[[ "$1" = "" ]] && \
  echo "Give me a path to the location where you want to have the database created" && \
  exit 1

export USER_NAME=tigase
export USER_PASS=${USER_NAME}
export ROOT_NAME=root
export ROOT_PASS=${ROOT_USER}
export DB_HOST=localhost
export DB_NAME=$1
export DB_TYPE=derby
export DB_VERSION=7-1

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL -file database/${DB_TYPE}-pubsub-schema-3.2.0.sql

echo -e "\n\n\nconfiguration:\n\n--user-db=derby\n--user-db-uri=jdbc:derby:$1\n\n"
