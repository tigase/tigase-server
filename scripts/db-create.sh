#!/bin/bash


if [ -z "${1}" ] ; then
  echo -e "Usage:"
  echo -e "$ scripts/db-create.sh"
  echo -e "\t -t database_type {derby, mysql, postgresql} "
  echo -e "\t[-v schema_version {4, 5, 5.1, 7.1} ]"
  echo -e "\t[-d database_name]"
  echo -e "\t[-h database hostname]"
  echo -e "\t[-u tigase_username]"
  echo -e "\t[-p tigase_userpass]"
  echo -e "\t[-r database root name]"
  echo -e "\t[-a database root pass]"

  exit 1
fi

create_database() {
  echo -e "database type:\t\t ${DB_TYPE}"
  echo -e "schema_version:\t\t ${VERSION}"
  echo -e "schema_file:\t\t database/mysql-schema-${FILE_VERSION}.sql"
  echo -e "database_name:\t\t ${DB_NAME}"
  echo -e "database_hostname:\t ${DB_HOST}"
  echo -e "tigase_username:\t ${USER_NAME}"
  echo -e "tigase_userpass:\t ${USER_PASS}"
  echo -e "database_root_name:\t ${ROOT_NAME}"
  echo -e "database_root_pass:\t ${ROOT_PASS}"

  read -p "Press [Enter] key to start, otherwise abort..."


java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname ${DB_HOST} -dbType ${DB_TYPE} -schemaVersion ${VERSION} -dbName ${DB_NAME} -rootUser ${ROOT_NAME} -rootPass ${ROOT_PASS} -dbUser ${USER_NAME} -dbPass ${USER_PASS} -logLevel ALL -file database/${DB_TYPE}-pubsub-schema-3.2.0.sql


  echo -e "\n\n\nconfiguration:\n\n--user-db=mysql\n--user-db-uri=jdbc:mysql://$DB_HOST:3306/$DB_NAME?user=$USER_NAME&password=$USER_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"

}



while getopts ":t:v:u:p:r:a:d:h:" optname
  do
    case "$optname" in
      "t")	DB_TYPE="${OPTARG}";;
      "v")	VERSION="${OPTARG}";;
      "d")	DB_NAME="${OPTARG}";;
      "h")	DB_HOST="${OPTARG}";;
      "u")	USER_NAME="${OPTARG}";;
      "p")	USER_PASS="${OPTARG}";;
      "r")	ROOT_NAME="${OPTARG}";;
      "a")	ROOT_PASS="${OPTARG}";;
      "?")	echo "Unknown option: $OPTARG";;
      ":")	echo "No argument value for option $OPTARG";;
      *)	echo "Unknown error while processing options";;
    esac
  done

#echo -e "\n"

if [ -z "${DB_TYPE}" ] ; then
  echo "No Database type specified - must be [derby,mysql,postgresql]; exiting"
  exit 1
fi

if [ -z "${DB_NAME}" ] ; then
  echo -e "No database_name given. Using: \t\ttigasedb"
  DB_NAME=tigasedb
fi

if [ -z "${DB_HOST}" ] ; then
  echo -e "No database_hostname given. Using: \tlocalhost"
  DB_HOST=localhost
fi

if [ -z "${USER_NAME}" ] ; then
  echo -e "No tigase_username given. Using: \ttigase_user"
  USER_NAME=tigase_user
fi

if [ -z "${USER_PASS}" ] ; then
  echo -e "No tigase_userpass given. Using: \ttigase_pass"
  USER_PASS=tigase_pass
fi

if [ -z "${ROOT_NAME}" ] ; then
  echo -e "No database_root_name given. Using: \troot"
  ROOT_NAME=root
fi

if [ -z "${ROOT_PASS}" ] ; then
  echo -e "No database_root_pass given. Using: \troot"
  ROOT_PASS=root
fi

if [ -z "${VERSION}" ] ; then
  echo -e "No schema_version given. Using latest: \t5.1"
  FILE_VERSION=7-1
  VERSION=7.1
else
  case "$VERSION" in
    "7.1")		FILE_VERSION="7-1";;
    "5.1")		FILE_VERSION="5-1";;
    "5")		FILE_VERSION="5";;
    "4")		FILE_VERSION="4";;
    "3")		FILE_VERSION="3";;
    *)			echo "unknown schema version $VERSION - must be [7.1, 5.1, 5, 4, 3]; exiting" ; exit 1;;
  esac
fi

echo ""


case "$DB_TYPE" in
  "derby")		create_database;;
  "mysql")		create_database;;
  "sqlserver")		create_database;;
  "postgresql")		create_database;;
  *)			echo "unknown database type $DB_TYPE - must be [derby,mysql,postgresql,sqlserver]; exiting" ; exit 1;;
esac
