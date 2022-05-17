.. _tigaseAuthConnector:

Tigase Auth Connector (DEPRECATED)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. **Warning**::

    Tigase Auth connector is **DEPRECATED** as of version 8.0.0 and will be removed in future releases

The Tigase Auth connector with shortcut name: **tigase-auth** is implemented in the class: `tigase.db.jdbc.TigaseAuth <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/db/jdbc/TigaseAuth.java>`__. It allows you to connect to any external database to perform user authentication. You can find more details how to setup a custom connector in the :ref:`Custom Authentication Connectors<customAuthentication>` guide.

To make this connector working you have to prepare your database to offer set of stored procedures for Tigase server to perform all the authentication actions. The best description is the example schema with all the stored procedures defined - please refer to the Tigase repositories for the schema definition files (each component has it’s dedicated schema). For example:

-  `tigase-server <https://github.com/tigase/tigase-server/tree/master/src/main/database>`__

-  `tigase-pubsub <https://github.com/tigase/tigase-pubsub/tree/master/src/main/database>`__

-  `tigase-muc <https://github.com/tigase/tigase-muc/tree/master/src/main/database>`__

-  `tigase-message-archiving <https://github.com/tigase/tigase-message-archiving/tree/master/src/main/database>`__

-  `tigase-socks5 <https://github.com/tigase/tigase-socks5/tree/master/src/main/database>`__

The absolute minimum of stored procedures you have to implement is:

-  ``TigUserLoginPlainPw`` - to perform user authentication. The procedure is always called when the user tries to login to the XMPP server. This is the only procedure which must be implemented and actually must work.

-  ``TigUserLogout`` - to perform user logout. The procedure is always called when the user logouts or disconnects from the server. This procedure must be implemented but it can be empty and can do nothing. It just needs to exist because Tigase expect it to exist and attempts to call it.

With these 2 above stored procedures you can only perform user login/logouts on the external database. You can’t register a user account, change user password or remove the user. In many cases this is fine as all the user management is handled by the external system.

If you however want to allow for account management via XMPP you have to implement also following procedures:

-  ``TigAddUserPlainPw`` - to add a new user account

-  ``TigRemoveUser`` - to remove existing user account

-  ``TigUpdatePasswordPlainPw`` - to change a user password for existing account

