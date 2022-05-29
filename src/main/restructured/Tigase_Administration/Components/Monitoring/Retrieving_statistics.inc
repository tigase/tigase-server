.. _Retrieving-statistics-from-the-server:

Retrieving statistics from the server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default we can retrieve server statistics using XMPP, no additional setup is necessary.

Retrieving statistics using XMPP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Accessing statistics over XMPP protocol requires any XMPP client capable of executing `XEP-0050: Ad-Hoc Commands <http://xmpp.org/extensions/xep-0050.html>`__. It’s essential to remember, that only administrator (a user whose JID is configured as administrative) can access the statistics.

Psi XMPP Client
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For the purpose of this guide `Psi <http://psi-im.org/>`__ client will be used. After successfully configuring and connecting to account with administrative privileges we need to access *Service Discovery*, either from application menu or from context menu of the particular account account:

|roster-discovery|

In the *Service Discovery* window we need to find *Server Statistics* component:

|discovery-stats|

We can either access statistics for all components or select particular component after expanding the tree. To execute ad-hoc command simply double click on the particular node which will open window with statistics:

|server-stats|

In this window, in addition to see the statistics, we can adjust *Stats level* by selecting desired level from the list and confirm by clicking *Finish*.

Retrieving statistics using JMX
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to access statistics over JMX we need to enable support for it in Tigase - `Monitoring Activation <#monitoring_activation>`__. Afterwards we can use a number of tools to get to the statistics, for example the following:

JConsole
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After opening JConsole we either select local process or provide details of the remote process, including IP, port and credentials from **etc/jmx.**\ \* files:

|jconsole|

Afterwards we navigate to the MBeans tab from where we can access the ``tigase.stats`` MBean. It offers similar options to XMPP - either accessing statistics for all components or only for particular component as well as adjusting level for which we want to obtain statistics:

|jconsole-1|


StatsDumper.groovy
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to collect statistics over period of time following groovy script can be used: `StatsDumper.groovy <https://raw.githubusercontent.com/tigase/tigase-server/master/src/main/restructured/files/StatsDumper.groovy>`__. It’s a Simple JMX client that connects to Tigase and periodically saves all statistics to files.

It takes following parameters:

.. code:: bash

   $ groovy StatsDumper.groovy [hostname] [username] [password] [dir] [port] [delay(ms)] [interval(ms)] [loadhistory(bool)]

-  ``hostname`` - address of the instance

-  ``username`` - JMX username

-  ``password`` - JMX username

-  ``dir`` - directory to which save the files with statistics

-  ``port`` - port on which to make the connection

-  ``delay``\ (ms) - initial delay in milliseconds after which statistics should be saved

-  ``interval``\ (ms) - interval between each retrieval/saving of statistics

-  ``loadhistory``\ (bool) - indicates whether or not load statistics history from server (if such is enabled in Tigase)

.. |roster-discovery| image:: /images/admin/monitoring_xmpp_1.png
.. |discovery-stats| image:: /images/admin/monitoring_xmpp_2.png
.. |server-stats| image:: /images/admin/monitoring_xmpp_3.png
.. |jconsole| image:: /images/admin/monitoring_jmx_jconsole_1.png
.. |jconsole-1| image:: /images/admin/monitoring_jmx_jconsole_2.png