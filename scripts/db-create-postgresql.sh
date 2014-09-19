#!/bin/bash

echo -e "\n\nusage: db-create-mysql.sh tigase_username tigase_password database_name database_host \n\n"
 
if [ "${1}" = "-y" ] ; then
  NONINTERACTIVE=yes
  shift
fi

if [ -z "${1}" ] ; then
  echo "No username given. Using: tigase_user"
  USR_NAME=tigase_user
else
  USR_NAME="${1}"
fi

if [ -z "${2}" ] ; then
  echo "No password given. Using: tigase_passwd"
  USR_PASS=tigase_passwd
else
  USR_PASS="${2}"
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
  echo "creating ${DB_NAME} database for user ${USR_NAME} identified by ${USR_PASS} password:"
  echo ""
 
  read -p "Press [Enter] key to start, otherwise abort..."
else
  echo "User: $USR_NAME, Pass: $USR_PASS, Db: $DB_NAME, Host: $DB_HOST"
fi

echo "Creating user"
createuser -d -S -R -h $DB_HOST -U postgres ${USR_NAME}
echo "Creating database"
createdb -h $DB_HOST -U ${USR_NAME} ${DB_NAME}
echo "Loading DB schema"
psql -h $DB_HOST -q -U ${USR_NAME} -d $DB_NAME -f database/postgresql-schema-5-1.sql
echo "Loading PubSub3 schema"
psql -h $DB_HOST -q -U ${USR_NAME} -d $DB_NAME -f database/postgresql-pubsub-schema-3.0.0.sql


echo -e "\n\n\nconfiguration:\n\n--user-db=pgsql\n--user-db-uri=jdbc:postgresql://$DB_HOST/$DB_NAME?user=$USR_NAME&password=$USR_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"
