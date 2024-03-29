.. _Configuration-of-statistics-loggers:

Configuration of statistics loggers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It is possible to enable and configure automatic storage of statistics information. To do that you need to configure any of following statistics loggers as a ``StatisticsCollector`` component sub-beans:

``tigase.stats.CounterDataArchivizer``
   every execution put current basic server metrics (CPU usage, memory usage, number of user connections, uptime) into database (overwrites previous entry).

``tigase.stats.CounterDataLogger``
   every execution insert new row with new set of number of server statistics (CPU usage, memory usage, number of user connections per connector, number of processed packets of different types, uptime, etc) into the database.

``tigase.stats.CounterDataFileLogger``
   every execution store all server statistics into separate file.

As an example to configure ``tigase.stats.CounterDataFileLogger`` to archive statistics data with level ``FINE`` every 60 seconds to file prefixed with ``stat`` and located in ``logs/server_statistics`` following entry is needed:

.. code::

   stats() {
       'stats-file-logger' (class: tigase.stats.CounterDataFileLogger) {
           'stats-directory' = 'logs/server_statistics'
           'stats-filename' = 'stat'
           'stats-unixtime' = false
           'stats-datetime' = true
           'stats-datetime-format' = 'HH:mm:ss'
           'stats-level' = 'FINEST'
       }
   }