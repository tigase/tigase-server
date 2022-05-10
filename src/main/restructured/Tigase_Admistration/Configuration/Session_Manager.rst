Session Manager
-----------------

Tigase Session Manager is where most of Tigase basic options can be configured, and where many operations are controlled from. Changes to session manager can effect operations throughout an entire XMPP installation, so care must be made when changing settings here.

.. _sessManMobileOpts:

Mobile Optimizations
^^^^^^^^^^^^^^^^^^^^^^

By default, Tigase employs XEP-0352 Client State Indication which allows for a more streamlined mobile experiencing by allowing the XMPP server to suppress or reduce the number of updates sent to a client thereby reducing the number of stanzas sent to a mobile client that is inactive. This employment is contained within the processor ``ClientStateIndication`` and is independent from the MobileV1, MobileV2, MobileV3 settings.

However, this can be fine tuned by using mobile plugins from Tigase which can be used at the same time by adding the following line to the ``config.tdsl`` file:

.. code:: properties

   }
   'sess-man' {
       'urn:xmpp:csi:0' {
           logic = 'tigase.xmpp.impl.MobileV1'
       }
   }

Logic Options are:

MobileV1
~~~~~~~~~~~~

Keeps all presence stanzas in queue until client is active.

::

   logic = 'tigase.xmpp.impl.MobileV1'


MobileV2
~~~~~~~~~~~~

This setting delays delivery of presences while client is in inactive state, but only keeps the last presence for each full jid. **This is the default setting for CSI logic**.

::

   logic = 'tigase.xmpp.impl.MobileV2'


MobileV3
~~~~~~~~~~~~

Keeps the same presence logic as MobileV2, but also queues Message Carbons. **Currently not supported by CSI processor, will cause issues**.

::

   logic = 'tigase.xmpp.impl.MobileV3'


Disabling CSI
~~~~~~~~~~~~~~~

If you wish to not use the ``ClientStateIndication`` processor, set the following in your ``config.tdsl`` file:

.. code:: dsl

   'sess-man' () {
       'urn:xmpp:csi:0' (active: false) {}
   }


A note about Mobile Plugins
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Previously, you could enable Mobile optimization logic using by enabling ``Mobile_V1 (){}`` bean to Session Manager: ``sess-man () {}`` bean.

If you have used these in the past, it is recommended you change your system to use the CSI processor with the appropriate mobile processing logic.

If you require v3 logic, or do not wish to use CSI, be sure to disable it using the above option.


threads-pool
^^^^^^^^^^^^^^^^

The ``threadsNo`` property allows you to fine-tune the SM plugin’s (processors) thread pool. With the default settings every plugin gets his own thread pool. This guarantees the best performance and optimal resource usage. The downside of this setting is that packets can arrive out of order if they are processed within different thread pools.

We can even fine tune this packet processing. Let’s say you want most of the plugins to be executed within a single thread pool to preserve packet ordering for them, but for some selected plugins that should execute within separate thread pools to improve performance. Let’s say, authentication packets and user registration can be actually executed in a separate thread pools as we do not worry about an order for them. Users cannot send or receive anything else before they authenticates anyway. The solution is to specify a number of threads for the selected plugin. For example, setting a common thread pool for all plugins but registration and authentication can be done with following configuration:

.. code:: dsl

   'sess-man' () {
       'amp' () {
           threadsNo = 30
       }
       'presence-state' () {
           threadsNo = 27
       }
   }

This replaces the old ``--sm-threads-pool`` property, as well as specifying thread pools in ``--sm-plugins``.


Thread Pool factor
^^^^^^^^^^^^^^^^^^^^^^^

Session manager can control the number of available thread pools for each processor. By adding the following line to the ``config.tdsl`` file, the global thread pool can be increased by a specified factor:

.. code:: dsl

   'sess-man' () {
       'sm-threads-factor' = 3
   }

In this case, the global thread pools is increased by a factor or 3.


Strategy
^^^^^^^^^^^^^^^^^^^^^^^

The ``Strategy`` property allows users to specify Clustering Strategy class which should be used for handling clustering environment; by default ``SMNonCachingAllNodes`` is used.

Any class implementing ``tigase.cluster.strategy.ClusteringStrategyIfc`` interface may be used for this setting.

Example:

.. code:: dsl

   'sess-man' () {
       strategy (class: tigase.cluster.strategy.SMCachingAllNodes)
   }

This replaces the old ``--sm-cluster-strategy-class`` setting from v7.1.