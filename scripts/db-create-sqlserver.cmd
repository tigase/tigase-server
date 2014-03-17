IF [%1]==[] (
	set servername=localhost
	set database=tigasedb
	set user=tigase
	set password=tigase12
	set root_user=sa
	set root_pass=sa
) ELSE (
	set servername=%1
	set database=%2
	set user=%3
	set password=%4
	set root_user=%5
	set root_pass=%6
)

sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE DATABASE [%database%]"
sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE LOGIN [%user%] WITH PASSWORD=N'%password%', DEFAULT_DATABASE=[%database%]"
sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -Q "ALTER AUTHORIZATION ON DATABASE::%database% TO %user%;"
sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-schema.sql
sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-sp.sql
sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-props.sql

