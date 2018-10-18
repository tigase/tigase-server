@echo off
set task=%1
set filename=%0

shift
shift
set args=%1
shift
:start
if [%1] == [] goto done
set args=%args% %1
shift
goto start

:schemaManager
java -cp "jars/*" -Djdbc.drivers=com.mysql.jdbc.Driver -DscriptName='%filename%' tigase.db.util.SchemaManager %task% %args%
goto finish

:done

set cmd = 
set class="tigase.server.XMPPServer"
if "%task%" == "upgrade-schema" goto schemaManager
if "%task%" == "install-schema" goto schemaManager
if "%task%" == "destroy-schema" goto schemaManager

java -cp "jars/*" -Djdbc.drivers=com.mysql.jdbc.Driver tigase.server.XMPPServer %args%

:finish