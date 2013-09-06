osql -U root -P root -i database\sqlserver-create_database.sql
osql -U root -P root -d tigasedb -i database\sqlserver-schema-5-1-schema.sql
osql -U root -P root -d tigasedb -i database\sqlserver-schema-5-1-sp.sql
osql -U root -P root -d tigasedb -i database\sqlserver-schema-5-1-props.sql
