Application passwords
-------------------------

In recent versions of Tigase XMPP Server it is possible to create and use multiple username and password pairs to authorize connection to the single XMPP account.

With that in place it is now possible to have multiple password for a multiple clients accessing the same account what can be used to increase security of the account as even if one password compromised you can still log in and block lost or compromised device.

Adding application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To add new username-password pair you need to execute ``Add user credentials`` ad-hoc command (command node ``auth-credentials-add`` at ``sess-man``) while logged in the XMPP account for which you want to add a new application password.

During execution for a command you will be provided with a form to fill in with following fields:

-  The Jabber ID for the account (``jid``) - bare JID of your account

-  Credential ID (``credentialId``) - username for the new application password

-  Password (``password``) - a new password

After submitting this form a new credential will be added.

Login in with application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To log in with new password the XMPP client can use any SASL mechanism but it needs to provide (in SASL message):

-  ``authzid`` - account JID

-  ``authcid`` - username for application password

-  ``passwd`` - application password

With proper values, you application will be able to log in using application password.

Removing application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If your device is compromised or lost and you want to remove application password, you need to use a different device and log in on your XMPP account. Then you need to execute ``Delete user credentials`` ad-hoc command (command node ``auth-credentials-delete`` at ``sess-man``).

During execution for a command you will be provided with a form to fill in with following fields:

-  The Jabber ID for the account (``jid``) - bare JID of your account

-  Credential ID (``credentialId``) - username for the application password which you want to remove

After submitting this form a credential will be removed.
