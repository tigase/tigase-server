.. _External-Component-Configuration:

External Component Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As for all Tigase components you can load and configure external components via the ``config.tdsl`` file described in details in the :ref:`DSL configuration<dslConfig>` section. This document describes how to enable the component and set the initial configuration to accept or initiate connections for an external component.

First thing to do is to specify the component class and the component name which must be unique within the Tigase installation. The most commonly name used is ``ext`` and the class is ``tigase.server.ext.ComponentProtocol`` (class doesn’t have to be specified when using default name).

The following line in the ``config.tdsl`` will load the component during the server startup time:

.. code::

   ext (class: tigase.server.ext.ComponentProtocol) {}

While this would load the component, without any additional configurations provided, the component would be practically useless. It is necessary to configure the virtual host domains of the external component during run-time via ad-hoc commands to make use of this component.

You may additionally configure the :ref:`bind-ext-hostnames<bindExtHostnames>` property.

To configure external component connections using Admin UI you need to open Admin UI web page (if you are logged in the same computer on which Tigase XMPP Server is running by default it should be available at http://localhost:8080/admin/). Then you should click on ``Configuration`` on the left side of the Admin UI web page and then select ``Add new item`` on ``ext`` component or by execution corresponding ad-hoc command on ``ext`` component using ad-hoc capable XMPP client, ie. `Psi <http://psi-im.org>`__.

|adminui ext add item button|

You will be presented with a form which you should fill to configure external component connection details:

|adminui ext add item form|

-  *Domain name* - external component domain name (``muc.devel.tigase.org``)

-  *Domain password* - password for authentication of the external component connection (``muc-pass``)

-  *Connection type* - ``accept`` to make component wait for connection or ``connect`` force component to connect to the server (``connect``)

-  *Port number* - port on which component should wait for connection or on which it try to connect (``5270``)

-  *Remote host* - host to connect to (``devel.tigase.org``) *(may be left blank if component will only accept connections)*

-  *Protocol* - id of protocol used for establishing connection

   -  if connection type is ``connect``:

      -  ``XEP-0114: Jabber Component Protocol (accept)`` - for `XEP-0114: Jabber Component Protocol <https://xmpp.org/extensions/xep-0114.html>`__

      -  ``XEP-0225: Component Connections`` - for `XEP-0225: Component Connections <https://xmpp.org/extensions/xep-0225.html>`__

   -  if connection type is ``accept``:

      -  ``Autodetect`` - for automatic detection of protocol used by incoming connection *(recommended)*

      -  ``XEP-0114: Jabber Component Protocol (accept)`` - for `XEP-0114: Jabber Component Protocol <https://xmpp.org/extensions/xep-0114.html>`__

      -  ``XEP-0225: Component Connections`` - for `XEP-0225: Component Connections <https://xmpp.org/extensions/xep-0225.html>`__

Additional options may be left with defaults.

Later on if you would like to modify this values, you can do that using Admin UI by clicking on ``Configuration`` and ``Remove an item`` or ``Update item configuration`` at ``ext`` component or by execution corresponding ad-hoc commands on ``ext`` component using ad-hoc capable XMPP client, ie. `Psi <http://psi-im.org>`__.

.. |adminui ext add item button| image:: /images/admin/adminui_ext_add_item_button.png
.. |adminui ext add item form| image:: /images/admin/adminui_ext_add_item_form.png
