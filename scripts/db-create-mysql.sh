#!/bin/bash

echo -e "\n\nusage: db-create-mysql.sh tigase_username tigase_password database_name root_username root_password database_host \n\n"

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
  echo "No DB root username given. Using: root"
  DB_USER=root
else
  DB_USER="${4}"
fi

if [ -z "${5}" ] ; then
  echo "No DB root password given. Using: root"
  DB_PASS=root
else
  DB_PASS="${5}"
fi

if [ -z "${6}" ] ; then
  echo "No DB hostname given. Using: localhost"
  DB_HOST=localhost
else
  DB_HOST="${6}"
fi

echo ""

echo "creating ${DB_NAME} database for user ${USR_NAME} identified by ${USR_PASS} password:"

echo ""

read -p "Press [Enter] key to start, otherwise abort..."


echo "DROP DATABASE IF EXISTS ${DB_NAME}" | mysql -h $DB_HOST -u $DB_USER -p$DB_PASS
echo "CREATE DATABASE ${DB_NAME}" | mysql -h $DB_HOST -u $DB_USER -p$DB_PASS

echo "GRANT ALL ON ${DB_NAME}.* TO ${USR_NAME}@'%' IDENTIFIED BY '${USR_PASS}';" | \
        mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME
echo "GRANT ALL ON ${DB_NAME}.* TO ${USR_NAME}@'localhost' IDENTIFIED BY '${USR_PASS}';" | \
        mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME
echo "GRANT ALL ON ${DB_NAME}.* TO ${USR_NAME} IDENTIFIED BY '${USR_PASS}';" | \
        mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME

echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USR_NAME}@'localhost';" | \
        mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME
echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USR_NAME}@'%';" | mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME
echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USR_NAME};" | mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME

echo "FLUSH PRIVILEGES;" | mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME

echo "Loading schema"

mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME < database/mysql-schema-5.1.sql


echo -e "\n\n\nconfiguration:\n\n--user-db=mysql\n--user-db-uri=jdbc:mysql://$DB_HOST:3306/$DB_NAME?user=$DB_USER&password=$DB_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"
