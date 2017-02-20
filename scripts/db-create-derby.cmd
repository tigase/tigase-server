@echo off

if [%1]==[] (
	echo. && echo Give me a path to the location where you want to have the database created && echo.
  exit /b
)

set PWD="%cd%"


set USER_NAME=tigase
set USER_PASS=%USER_NAME%
set ROOT_NAME=root
set ROOT_PASS=%ROOT_USER%
set DB_HOST=localhost
set DB_NAME=%1
set DB_TYPE=derby
set DB_VERSION=7-1

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname %DB_HOST% -dbType %DB_TYPE% -schemaVersion %DB_VERSION% -dbName %DB_NAME% -rootUser %ROOT_NAME% -rootPass %ROOT_PASS% -dbUser %USER_NAME% -dbPass %USER_PASS% -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname %DB_HOST% -dbType %DB_TYPE% -schemaVersion %DB_VERSION% -dbName %DB_NAME% -rootUser %ROOT_NAME% -rootPass %ROOT_PASS% -dbUser %USER_NAME% -dbPass %USER_PASS% -logLevel ALL -file database/%DB_TYPE%-pubsub-schema-3.2.0.sql




if %errorlevel% neq 0 (
  echo. && echo Error: please check the logs for more details && echo. && echo.
  exit /b %errorlevel%
) else (
  echo. && echo Success: please look at the logs for more details && echo. && echo.
  echo configuration: && echo. && echo. && echo --user-db=derby && echo. && echo --user-db-uri=jdbc:derby:%1 && echo. && echo.
  exit /b
)
