PubSub Server Information
----------------------------

Tigase offers support for `PubSub Server Information extension <https://github.com/xsf/xeps/pull/1312>`_ of the XMPP. This feature allows your server to expose its server information over Pub/Sub allowing it to be included ie. in the XMPP network graph.

This plugin, if enabled, announces support for PubSub Server Information extension and acts as opt-in.

In the future this feature will be extended to expose additional server information, ie. currently open server-to-server connections (only made to servers that also opted in), etc.

Enabling feature
^^^^^^^^^^^^^^^^^^

To enable this feature, you need to modify ``config.tdsl`` file by adding folliwing line ``'urn:xmpp:serverinfo:0' () {}`` to the ``'sess-man'`` section of the config file.

Your ``'sess-man'`` section, should include this new line, like in the following example:

.. code::

   'sess-man' {
       'urn:xmpp:serverinfo:0' () {}
   }
