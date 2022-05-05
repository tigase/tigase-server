Data Source and Repositories
================================

In Tigase XMPP Server 8.0.0 a new concept of data sources was introduced. It was introduced to create distinction between classes responsible for maintaining connection to actual data source and classes operating on this data source.

Data sources
--------------

|Relations between DataSourceBean and DataSources|

DataSource
^^^^^^^^^^^^^^

``DataSource`` is an interface which should be implemented by all classes implementing access to data source, i.e. implementing access to database using JDBC connection or to MongoDB. Implementation of ``DataSource`` is automatically selected using uri provided in configuration and ``@Repository.Meta`` annotation on classes implementing ``DataSource`` interface.

DataSourcePool
^^^^^^^^^^^^^^^

``DataSourcePool`` is interface which should be implemented by classes acting as a pool of data sources for single domain. There is no requirement to create class implementing this interface, however if implementation of ``DataSource`` is blocking and does not support concurrent requests, then creation of ``DataSourcePool`` is recommended. An example of such case is implementation of ``DataRepositoryImpl`` which executes all requests using single connection and for this class there is ``DataRepositoryPool`` implementing ``DataSourcePool`` interface and improving performance. Implementation of ``DataSourcePool`` is automatically selected using uri provided in configuration and ``@Repository.Meta`` annotation on classes implementing ``DataSourcePool`` interface.

DataSourceBean
^^^^^^^^^^^^^^^

This class is a helper class and provides support for handling multiple data sources. You can think of a ``DataSourceBean`` as a map of named ``DataSource`` or ``DataSourcePool`` instances. This class is also responsible for initialization of data source. Moreover, if data source will change during runtime ``DataSourceBean`` is responsible for firing a ``DataSourceChangedEvent`` to notify other classes about this change.

User and authentication repositories
------------------------------------------

This repositories may be using existing (configured and initialized) data sources. However, it is also possible to that they may have their own connections. Usage of data sources is recommended if possible.

|Relations between AuthRepositories and DataSources|

|Relations between UserRepositories and DataSources|

AuthRepository and UserRepository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This are a base interfaces which needs to be implemented by authentication repository (``AuthRepository``) and by repository of users (``UserRepository``). Classes implementing this interfaces should be only responsible for retrieving data from data sources.

AuthRepositoryPool and UserRepositoryPool
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If class implementing ``AuthRepositoryPool`` or ``UserRepositoryPool`` is not using data sources or contains blocking or is not good with concurrent access, then it should be wrapped within proper repository pool. Most of implementations provided as part of Tigase XMPP Server do not require to be wrapped within repository pool. If your implementation is blocking or not perform well with concurrent access (ie. due to synchronization), then it should be wrapped within this pool. To wrap implementation within a pool, you need to set ``pool-cls`` property of configured user or authentication repository in your configuration file.

AuthRepositoryMDPoolBean and UserRepositoryMDPoolBean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This classes are for classes implementing ``AuthRepository`` and ``UserRepository`` what ``DataSourceBean`` is for classes implementing ``DataSource`` interface. This classes holds map of named authentication or user repositories. They are also responsible for initialization of classes implementing this repositories.


Other repositories
-----------------------

It is possible to implement repositories not implementing ``AuthRepository`` or ``UserRepository``. Each type of custom repository should have its own API and its own interface.

|Relations between custom repositories and DataSources|

DataSourceAware
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Custom repositories should implement they own interface specifying its API. This interfaces should extend ``DataSourceAware`` interface which is base interface required to be implemented by custom repositories. ``DataSourceAware`` has a method ``setDataSource()`` which will be called with instance of data source to initialize instance of custom repository. Implementations should be annotated with ``@Repository.Meta`` implementation to make the automatically selected for proper type of ``DataSource`` implementation.

MDRepositoryBean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It is required to create a class extending ``MDRepositoryBean`` implementing same custom interface as the custom repository. This class will be a multi domain pool, allowing you to have separate implementation of custom repository for each domain. Moreover, it will be responsible for creation and initialization of your custom repository instances.

.. |Relations between DataSourceBean and DataSources| image:: ../../asciidoc/devguide/images/datasourcebean-datasources.png
.. |Relations between AuthRepositories and DataSources| image:: ../../asciidoc/devguide/images/datasource-authrepository.png
.. |Relations between UserRepositories and DataSources| image:: ../../asciidoc/devguide/images/datasource-userrepository.png
.. |Relations between custom repositories and DataSources| image:: ../../asciidoc/devguide/images/datasource-customrepository.png
