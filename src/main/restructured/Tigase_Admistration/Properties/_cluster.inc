Cluster
-----------

cl-comp
^^^^^^^^^

**Description:** Container specifying cluster component configuration.

**Default value:** By default, the cl-comp container is not listed in the ``config.tdsl`` file.

**Example:**

.. code::

   'cl-comp' {
       <configuration>
   }

connect-all
~~~~~~~~~~~~~~~

The ``cluster-connect-all`` property is used to open active connections to all nodes listed in the `cluster-nodes <#clusterNodes>`__ configuration property. This property should be used only on the node which is added to the live cluster at later time. Normally this new cluster node is not listed in the configuration of the existing cluster nodes. This is why they can not open connections the new node. The new node opens connection to all existing nodes instead. False is the default value and you can skip this option if you want to have it switched off which it is by default.

**Example**

.. code::

   'cl-comp' {
       'connect-all' = true
   }

This replaces the ``--cluster-connect-all`` property.

**Available since:** 8.0.0

cluster-mode
^^^^^^^^^^^^^^^^^^

**Description:** The property is used to switch cluster mode on. The default value is ``false`` so you can normally skip the parameter if you don’t want the server to run in cluster mode. You can run the server in the cluster mode even if there is only one node running. The performance impact is insignificant and you will have the opportunity to connect mode cluster nodes at any time without restarting the server.

**Default value:** ``false`` Tigase by default does not run in clustered mode.

**Example:** ``'cluster-mode' = 'true'``

**Possible values:** ``true|false``

**Available since:** 8.0.0

cluster-nodes
^^^^^^^^^^^^^^^^^^

**Default value:** none

**Example:** ``'cluster-nodes' = [ 'node1.domain:pass:port' , 'node2.domain:pass:port' , 'node3.domain:pass:port' ]``

**Possible values:** a comma separated list of hostnames.

**Description:** The property is used to specify a list of cluster nodes running on your installation. The node is the full DNS name of the machine running the node. Please note the proper DNS configuration is critical for the cluster to work correctly. Make sure the 'hostname' command returns a full DNS name on each cluster node. Nodes don’t have to be in the same network although good network connectivity is also a critical element for an effective cluster performance.

All cluster nodes must be connected with each other to maintain user session synchronization and exchange packets between users connected to different nodes. Therefore each cluster node opens a 'cluster port' on which it is listening for connections from different cluster nodes. As there is only one connection between each two nodes Tigase server has to decide which nodes to connect to and which has to accept the connection. If you put the same list of cluster nodes in the configuration for all nodes this is not a problem. Tigase server has a way to find and void any conflicts that are found. If you however want to add a new node later on, without restarting and changing configuration on old nodes, there is no way the old nodes will try to establish a connection to the new node they don’t know them. To solve this particular case the next parameter is used.

.. Note::

   Cluster nodes are not required to be configured, as they can automatically find/add/remove cluster nodes. This is for installations where nodes will be limited and static!

**Available since:** 8.0.0
