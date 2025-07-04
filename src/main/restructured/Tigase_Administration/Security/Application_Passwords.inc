
.. _application-passwords-admin-guide:

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

.. code::

    <iq type='set' to='sess-man@example.com' id='sasl-app-add-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-add' action='execute'/>
    </iq>

    <iq type='result' from='sess-man@example.com' id='sasl-app-add-1' to='user@example.com/resource-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-add' session-id='uuid-xxxxxx' status='executing'>
            <x xmlns='jabber:x:data' type='form'>
                <title>Add user credentials"</title>
                <field var='jid' label='The Jabber ID for the account' type='jid-single'/>
                <field var='credentialId' label='Credential ID' type='jid-single'/>
                <field var='password' label='Password' type='text-single'/>
            </x>
        </command>
    </iq>


After submitting this form a new credential will be added.

.. code::

    <iq type='set' to='sess-man@example.com' id='sasl-app-add-2'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-add' action='execute'>
            <x xmlns='jabber:x:data' type='submit'>
                <title>Add user credentials"</title>
                <field var='jid' label='The Jabber ID for the account' type='jid-single'>
                    <value>user@example.com</value>
                </field>
                <field var='credentialId' label='Credential ID' type='jid-single'>
                    <value>my-new-app-1</value>
                </field>
                <field var='password' label='Password' type='text-single'>
                    <value>39jfnwu053743</value>
                </field>
            </x>
        </command>
    </iq>

    <iq type='result' from='sess-man@example.com' id='sasl-app-add-2' to='user@example.com/resource-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-add' session-id='uuid-xxxxxx' status='completed'>
            <x xmlns='jabber:x:data' type='result'>
                <field type='fixed'>
                    <value>OK</value>
                </field>
            </x>
        </command>
    </iq>


Login in with application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To log in with new password the XMPP client can use any SASL mechanism but it needs to provide (in SASL message):

-  ``authzid`` - account JID

-  ``authcid`` - username for application password

-  ``passwd`` - application password

With proper values, you application will be able to log in using application password.

In case of SASL PLAIN which has the following format (spaces should be ommited and ``[]`` means it is optional):
``[authzid] UTF8NUL authcid UTF8NUL passwd``
not encoded payload would look like this:
``user@example.com UTF8NUL my-new-app-1 UTF8NUL 39jfnwu053743``

That after Base64 encoding would be presented as `dXNlckBleGFtcGxlLmNvbQBteS1uZXctYXBwLTEDOWpmbnd1MDUzNzQz` and this value can be used as a correct CData of `<auth/>` element:

.. code::

    <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>dXNlckBleGFtcGxlLmNvbQBteS1uZXctYXBwLTEDOWpmbnd1MDUzNzQz</auth>



Removing application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If your device is compromised or lost and you want to remove application password, you need to use a different device and log in on your XMPP account. Then you need to execute ``Delete user credentials`` ad-hoc command (command node ``auth-credentials-delete`` at ``sess-man``).

During execution for a command you will be provided with a form to fill in with following fields:

-  The Jabber ID for the account (``jid``) - bare JID of your account

-  Credential ID (``credentialId``) - username for the application password which you want to remove

.. code:

    <iq type='set' to='sess-man@example.com' id='sasl-app-delete-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-delete' action='execute'/>
    </iq>

    <iq type='result' from='sess-man@example.com' id='sasl-app-delete-1' to='user@example.com/resource-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-delete' session-id='uuid-xxxxxx' status='executing'>
            <x xmlns='jabber:x:data' type='form'>
                <title>Add user credentials"</title>
                <field var='jid' label='The Jabber ID for the account' type='jid-single'/>
                <field var='credentialId' label='Credential ID' type='jid-single'/>
            </x>
        </command>
    </iq>

After submitting this form a credential will be removed.

.. code::

    <iq type='set' to='sess-man@example.com' id='sasl-app-delete-2'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-delete' action='execute'>
            <x xmlns='jabber:x:data' type='submit'>
                <title>Add user credentials"</title>
                <field var='jid' label='The Jabber ID for the account' type='jid-single'>
                    <value>user@example.com</value>
                </field>
                <field var='credentialId' label='Credential ID' type='jid-single'>
                    <value>my-new-app-1</value>
                </field>
            </x>
        </command>
    </iq>

    <iq type='result' from='sess-man@example.com' id='sasl-app-delete-2' to='user@example.com/resource-1'>
        <command xmlns='http://jabber.org/protocol/commands' node='auth-credentials-delete' session-id='uuid-xxxxxx' status='completed'>
            <x xmlns='jabber:x:data' type='result'>
                <field type='fixed'>
                    <value>OK</value>
                </field>
            </x>
        </command>
    </iq>