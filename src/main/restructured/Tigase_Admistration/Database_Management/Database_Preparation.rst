Database Preparation
-----------------------

Tigase uses generally the same database schema and the same set of stored procedures and functions on every database. However, the schema creation scripts and code for stored procedures is different for each database. Therefore the manual process to prepare database is different for each database system.

Starting with v8.0.0, most of the database tasks have been automated and can be called using simple text, or using interactive question and answer style. We **DO NOT RECOMMEND** going through manual operation, however we have kept manual activation of different databases to the Appendix. If you are interested in how we manage and update our database schemas, you may visit the ` Schema files maintenance <#Schema-files-maintenance>`__ section of our Redmine installation for more detailed information.

-  `The DBSchemaLoader Utility <#Schema-Utility>`__

-  `Hashed User Passwords in Database <#Hashed-User-Passwords-in-Database>`__

-  `Support for MongoDB <#Preparing-Tigase-for-MongoDB>`__

Appendix entries

-  `Manual installtion for MySQL <#Prepare-the-MySQL-Database-for-the-Tigase-Server>`__

-  `Manual installtion for Derby <#Prepare-the-Derby-Database-for-the-Tigase-Server>`__

-  `Manual installtion for SQLServer <#Prepare-the-MS-SQL-Server-Database-for-the-Tigase-Server>`__

-  `Manual installtion for PostGRESQL <#Prepare-the-PostgreSQL-Database-for-the-Tigase-Server>`__

.. include:: Database_Preparation/DbSchemaLoader.rst

.. include:: Database_Preparation/MySQL.rst

.. include:: Database_Preparation/Derby.rst

.. include:: Database_Preparation/MSSQL.rst

.. include:: Database_Preparation/PostGRE.rst

.. include:: Database_Preparation/MongoDB.rst