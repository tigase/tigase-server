Database Preparation
-----------------------

Tigase uses generally the same database schema and the same set of stored procedures and functions on every database. However, the schema creation scripts and code for stored procedures is different for each database. Therefore the manual process to prepare database is different for each database system.

Starting with v8.0.0, most of the database tasks have been automated and can be called using simple text, or using interactive question and answer style. We **DO NOT RECOMMEND** going through manual operation, however we have kept manual activation of different databases to the Appendix. If you are interested in how we manage and update our database schemas, you may visit the :ref:`Schema files maintenance<Schemafilesmaintenance>` section of our Redmine installation for more detailed information.

-  :ref:`The DBSchemaLoader Utility<SchemaUtility>`

-  :ref:`Hashed User Passwords in Database<HashedUserPasswordsinDatabase>`

-  :ref:`Support for MongoDB<PreparingTigaseforMongoDB>`

Appendix entries

-  :ref:`Manual installtion for MySQL<Prepare-the-MySQL-Database-for-the-Tigase-Server>`

-  :ref:`Manual installtion for Derby<Prepare-the-Derby-Database-for-the-Tigase-Server>`

-  :ref:`Manual installtion for SQLServer<Prepare-the-MS-SQL-Server-Database-for-the-Tigase-Server>`

-  :ref:`Manual installtion for PostGRESQL<Prepare-the-PostgreSQL-Database-for-the-Tigase-Server>`

.. include:: Database_Preparation/DbSchemaLoader.inc

.. include:: Database_Preparation/MySQL.inc

.. include:: Database_Preparation/Derby.inc

.. include:: Database_Preparation/MSSQL.inc

.. include:: Database_Preparation/PostGRE.inc

.. include:: Database_Preparation/MongoDB.inc