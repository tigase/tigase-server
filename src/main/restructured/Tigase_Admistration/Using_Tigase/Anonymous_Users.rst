Anonymous Users & Authentication
-------------------------------------

To support anonymous users, you must first enable anonymous authentication on your server.

Anonymous Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase Server can support anonymous logins via SASL-ANONYMOUS in certain scenarios. This can be enabled on per-VHost basis by adjusting *Anonymous enabled* option as described in ` Add and Manage Domains (VHosts)<#Add-and-Manage-Domains-(VHosts)>`__ This setting is false by default as SASL-ANONYMOUS may not be totally secure as users can connect without prior permission (username and password).

Anonymous User Features
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To connect to your server anonymously, you must use a client that supports anonymous authentication and users. Connect to the server with the name of the server as the username, and no password. For example, to connect anonymously to ``xmpp.example.com`` use the following credentials,

Username: ``xmpp.example.com`` Password:

In this mode all login information is stored in memory, and cannot be retrieved at a later date.

Other features of Anonymous Authentication

-  Temporary Jid is assigned and destroyed the moment of login/logout.

-  Anonymous users cannot access the database

-  Anonymous users cannot communicate outside the server (use s2s connections)

-  Anonymous users have a default limit on traffic generated per user.

Reconnection on Anonymous
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

On products such as our JaXMPP Server, users connected using SASL-ANONYMOUS can reconnect to existing sessions using cookie management. However, reconnection can be improved and extended using `Bosh Session Cache <http://docs.tigase.org/tigase-server/snapshot/Development_Guide/html/#boshsessioncache>`__ which allows for session storage in memory rather than using client-side data for reconnection.