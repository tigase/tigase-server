#!/bin/bash


if [ -z "${1}" ] ; then
  echo -e "Usage:"
  echo -e "$ scripts/db-create.sh"
  echo -e "\t -t database_type {derby, mysql, postgresql} "
  echo -e "\t[-v schema_version {4, 5, 5.1} ]"
  echo -e "\t[-d database_name]"
  echo -e "\t[-h database hostname]"
  echo -e "\t[-u tigase_username]"
  echo -e "\t[-p tigase_userpass]"
  echo -e "\t[-r database root name]"
  echo -e "\t[-a database root pass]"

  exit 1
fi

create_derby() {
  echo -e "database type:\t\t ${DB_TYPE}"
  echo -e "schema_version:\t\t ${VERSION}"
  echo -e "schema_file:\t\t database/derby-schema-${FILE_VERSION}.sql"
  echo -e "database_name:\t\t ${DB_NAME}"
  
  read -p "Press [Enter] key to start, otherwise abort..."

  java -Dij.protocol=jdbc:derby: -Dij.database="${DB_NAME};create=true" \
		  -Dderby.system.home=`pwd` \
		  -cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar \
		  org.apache.derby.tools.ij database/derby-schema-${FILE_VERSION}.sql

  echo -e "\n\n\nconfiguration:\n\n--user-db=derby\n--user-db-uri=jdbc:derby:${DB_NAME}\n\n"
}

create_mysql() {
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

  echo "DROP DATABASE IF EXISTS ${DB_NAME}" | mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS
  echo "CREATE DATABASE ${DB_NAME}" | mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS

  echo "GRANT ALL ON ${DB_NAME}.* TO ${USER_NAME}@'%' IDENTIFIED BY '${USER_PASS}';" | \
	  mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME
  echo "GRANT ALL ON ${DB_NAME}.* TO ${USER_NAME}@'localhost' IDENTIFIED BY '${USER_PASS}';" | \
	  mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME
  echo "GRANT ALL ON ${DB_NAME}.* TO ${USER_NAME} IDENTIFIED BY '${USER_PASS}';" | \
	  mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME

  echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USER_NAME}@'localhost';" | \
	  mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME
  echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USER_NAME}@'%';" | mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME
  echo "GRANT SELECT, INSERT, UPDATE ON mysql.proc TO ${USER_NAME};" | mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME

  echo "FLUSH PRIVILEGES;" | mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME

  echo "Loading schema"

  mysql -h $DB_HOST -u $ROOT_NAME -p$ROOT_PASS $DB_NAME < database/mysql-schema-${FILE_VERSION}.sql

  echo -e "\n\n\nconfiguration:\n\n--user-db=mysql\n--user-db-uri=jdbc:mysql://$DB_HOST:3306/$DB_NAME?user=$USER_NAME&password=$USER_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"

}

create_postgresql() {
  echo -e "database type:\t\t ${DB_TYPE}"
  echo -e "schema_version:\t\t ${VERSION}"
  echo -e "schema_file:\t\t database/postgresql-schema-${FILE_VERSION}.sql"
  echo -e "database_name:\t\t ${DB_NAME}"
  echo -e "database_hostname:\t\t ${DB_HOST}"
  echo -e "tigase_username:\t\t ${USER_NAME}"
  echo -e "tigase_userpass:\t\t ${USER_PASS}"
  echo -e "database_root_name:\t\t ${ROOT_NAME}"
  echo -e "database_root_pass:\t\t ${ROOT_PASS}"

  read -p "Press [Enter] key to start, otherwise abort..."

  export PGPASSWORD=${ROOT_PASS}
  echo "CREATE ROLE ${USER_NAME} WITH PASSWORD '${USER_PASS}' NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT LOGIN;" | psql -h $DB_HOST -U ${ROOT_NAME}
  echo "CREATE DATABASE ${DB_NAME} OWNER ${USER_NAME} ENCODING 'UTF8' TEMPLATE template0;" | psql -h $DB_HOST -U ${ROOT_NAME}

  export PGPASSWORD=${USER_PASS}
  psql -h $DB_HOST -q -U ${USER_NAME} -d $DB_NAME -f database/postgresql-schema-${FILE_VERSION}.sql

  echo -e "\n\n\nconfiguration:\n\n--user-db=pgsql\n--user-db-uri=jdbc:postgresql://$DB_HOST/$DB_NAME?user=$USER_NAME&password=$USER_PASS&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true\n\n"
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
  FILE_VERSION=5-1
  VERSION=5.1
else
  case "$VERSION" in
    "5.1")		FILE_VERSION="5-1";;
    "5")		FILE_VERSION="5";;
    "4")		FILE_VERSION="4";;
    "3")		FILE_VERSION="3";;
    *)			echo "unknown schema version $VERSION - must be [5.1, 5, 4, 3]; exiting" ; exit 1;;
  esac
fi

echo ""


case "$DB_TYPE" in
  "derby")		create_derby;;
  "mysql")		create_mysql;;
  "postgresql")		create_postgresql;;
  *)			echo "unknown database type $DB_TYPE - must be [derby,mysql,postgresql]; exiting" ; exit 1;;
esac
