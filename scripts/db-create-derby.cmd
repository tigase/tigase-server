@echo off

if [%1]==[] (
	echo. && echo Give me a path to the location where you want to have the database created && echo.
  exit /b
)

set PWD="%cd%"

:: for tigase 5.0 and below
::java -Dij.protocol=jdbc:derby: -Dij.database="%1;create=true" ^
::		-Dderby.system.home=%PWD% ^
::		-classpath "libs/*" ^
::		org.apache.derby.tools.ij database/derby-schema-4.sql
::java -Dij.protocol=jdbc:derby: -Dij.database="%1" ^
::		-Dderby.system.home=%PWD% ^
::		-classpath "libs/*" ^
::		org.apache.derby.tools.ij database/derby-schema-4-sp.schema
::java -Dij.protocol=jdbc:derby: -Dij.database="%1" ^
::		-Dderby.system.home=%PWD% ^
::		-classpath "libs/*" ^
::		org.apache.derby.tools.ij database/derby-schema-4-props.sql

:: for Tigase 5.1

java -Dij.protocol=jdbc:derby: -Dij.database="%1;create=true" ^
		-Dderby.system.home=%PWD% ^
		-classpath jars/* ^
		org.apache.derby.tools.ij database/derby-schema-5-1.sql > derby-db-create.txt 2>&1

if %errorlevel% neq 0 (
  echo. && echo Error: please check the derby-db-create.txt error file for more details && echo. && echo.
  exit /b %errorlevel%
) else (
  echo. && echo Success: please look at the derby-db-create.txt file for more details && echo. && echo.
  echo configuration: && echo. && echo. && echo --user-db=derby && echo. && echo --user-db-uri=jdbc:derby:%1 && echo. && echo.
  exit /b
)
