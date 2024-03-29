Shutting Down Tigase
----------------------

Although Tigase XMPP Server can be terminated by ending the process, it is preferred and recommended to use it’s own shutdown scripts instead. Not only does this allow for a proper purge of Tigase form the system, but allows for all shutdown functions to operate, such as amending logs and completing statistics. To trigger a shutdown of Tigase server, the following command can be used from the tigase directory:

.. code:: bash

   ./scripts/tigase.sh stop

You may specify the config file if you want, but it will make no differences

This will:

-  Begin shutdown thread

-  Stop accepting new connections

-  Close all current connections

-  Collect runtime statistics

-  Write statistics to log

-  Dump full stacktrace to a file

-  Run GC and clear from memory


Shutdown statistics
^^^^^^^^^^^^^^^^^^^^^^^

Upon shutdown, statistics for the server’s runtime will be appended to the log file. For a description of the statistics and what they mean, refer to the :ref:`Statistics Description<statsticsDescription>` portion of the documentation.

Shutdown StackTrace Dump
^^^^^^^^^^^^^^^^^^^^^^^^^^

To aid with troubleshooting purposes, the full stacktrace will be dumped to a seperate file located at $serverdir/logs/threads-dump.log.# Stacktrace logs will follow the same log file numbering scheme described in :ref:`Log file description<Tigase-Log-Guide>`.

This feature is enabled by default, however you may disable this by adding the following to your ``config.tdsl`` file:

.. code::

   'shutdown-thread-dump' = false

Shutting Down Cluster Nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Starting with v8.0.0 you can now shut down individual cluster nodes without shutting down the whole server. This command will use the *SeeOtherHost* strategy to direct traffic to other nodes and update the cluster map to gracefully shut down the single node

Shutting down individual nodes can be done VIA Ad-hoc command and fill out the response forms. The command is available from message-router as http://jabber.org/protocol/admin#shutdown.