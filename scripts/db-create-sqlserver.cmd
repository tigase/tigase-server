IF [%1]==[] (
	set database=tigasedb
	set user=tigase
	set password=tigase12
	set root_user=root
	set root_pass=root
) ELSE (
	set database=%1
	set user=%2
	set password=%3
	set root_user=%4
	set root_pass=%5
)

sqlcmd -U %root_user% -P %root_pass% -Q "CREATE DATABASE [%database%]"
sqlcmd -U %root_user% -P %root_pass% -Q "CREATE LOGIN [%user%] WITH PASSWORD=N'%password%', DEFAULT_DATABASE=[%database%]"
sqlcmd -U %root_user% -P %root_pass% -d %database% -Q "ALTER AUTHORIZATION ON DATABASE::%database% TO %user%;"
sqlcmd -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-schema.sql
sqlcmd -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-sp.sql
sqlcmd -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-5-1-props.sql

