IF [%1]==[] (
	set DB_HOST=localhost
	set DB_NAME=tigasedb
	set USER_NAME=tigase
	set USER_PASS=tigase12
	set ROOT_NAME=sa
	set ROOT_PASS=sa
) ELSE (
	set DB_HOST=%1
	set DB_NAME=%2
	set USER_NAME=%3
	set USER_PASS=%4
	set ROOT_NAME=%5
	set ROOT_PASS=%6
)

set DB_TYPE=sqlserver
set DB_VERSION=7-1

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname %DB_HOST% -dbType %DB_TYPE% -schemaVersion %DB_VERSION% -dbName %DB_NAME% -rootUser %ROOT_NAME% -rootPass %ROOT_PASS% -dbUser %USER_NAME% -dbPass %USER_PASS% -logLevel ALL

java -cp "jars/*" tigase.util.DBSchemaLoader -dbHostname %DB_HOST% -dbType %DB_TYPE% -schemaVersion %DB_VERSION% -dbName %DB_NAME% -rootUser %ROOT_NAME% -rootPass %ROOT_PASS% -dbUser %USER_NAME% -dbPass %USER_PASS% -logLevel ALL -file database/%DB_TYPE%-pubsub-schema-3.2.0.sql


if %errorlevel% neq 0 (
  echo. && echo Error: please check the logs for more details && echo. && echo.
  exit /b %errorlevel%
) else (
  echo. && echo Success: please look at the logs for more details && echo. && echo.
  exit /b
)
