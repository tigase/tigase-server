.. _using-data-repository:

Using ``DataRepository``
========================

In order to use ``DataRepository`` the class has to implement ``DataSourceAware<DataRepository>`` interface and it's only API method: ``tigase.db.DataSourceAware.setDataSource``. In it you will get reference to an instance of the data repository (pool) and you should initialise all the prepared statements that you would like to use:

.. code:: java

    private static final String GET_DATA = "SELECT field FROM my_custom_table WHERE user_id = ?";

    private DataRepository data_repo = null;

    @Override
    public void setDataSource(DataRepository dataSource) throws RepositoryException {
        this.data_repo = dataSource;
        try {
            data_repo.initPreparedStatement("GET_DATA", GET_DATA);
        } catch (SQLException e) {
            log.log(Logger.Level.WARNING, "Failed to init prepared statement: " + e.getMessage());
        }
    }


To use such initialised data source you should obtain the prepared statement from it, set all the required parameters, execute it and retrieve the data from the ResultSet. It's essential to always close the result set:

.. code:: java

    public void getDataFromRepo(String userID) throws TigaseDBException {
        try {
            var getDataQuery = data_repo.getPreparedStatement(null, GET_DATA);
            getDataQuery.setString(1, userID);
            ResultSet rs = null;
            synchronized (getDataQuery) {
                try {
                    rs = getDataQuery.executeQuery();
                    if (rs.next()) {
                        log.log(Logger.Level.INFO, "User data: " + rs.getString(1));
                    }
                } finally {
                    data_repo.release(null, rs);
                }
            }
        } catch (SQLException e) {
            throw new TigaseDBException("Failed to get prepared statement: " + e.getMessage());
        }
    }

Complete source example is included in the sources as ``tigase.examples.ExampleUsingDataRepository``