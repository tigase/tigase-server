VHost / domain
--------------------

vhost-anonymous-enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``true``

**Example:** ``'vhost-anonymous-enabled' = 'false'``

**Possible values:** ``true|false``

**Description:** The ``vhost-anonymous-enabled`` property specifies whether anonymous user logins are allowed for the installation for all vhosts.

This is a global property which is overridden by settings for particular VHost (see :ref:`Add and Manage Domains (VHosts)<addManageDomain>`).

Default settings for all virtual hosts are used when this property is not defined. This settings is useful mostly for installations with many virtual hosts listed in the ``config.tdsl`` file for which there is no individual settings specified. It allows the configuration of default values for all of them, instead of having to provide individual configuration for each VHost.

**Available since:** 8.0.0

vhost-disable-dns-check
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'vhost-disable-dns-check' = 'true'``

**Possible values:** ``true|false``

**Description:** This property disables DNS validation when adding or editing vhosts in Tigase server. This also exempts administrative accounts from validation. With this property enabled, you will not benefit from seeing if proper SRV records are set so other people can connect to specific vhosts from outside your network.

This is a global property which is overridden by settings for particular VHost (see :ref:`Add and Manage Domains (VHosts)<addManageDomain>`).

**Available since:** 8.0.0

vhost-max-users
^^^^^^^^^^^^^^^^^^

**Default value:** ``0``

**Example:** ``'vhost-max-users' = '1000'``

**Possible values:** integer number.

**Description:** The ``vhost-max-users`` property specifies how many user accounts can be registered on the installations for all vhosts.

**0 - zero** means unlimited and this is a default. Otherwise greater than zero value specifies accounts number limit.

This is a global property which is overridden by settings for particular vhost.

The default setting is used for all virtual hosts for which the configuration is not defined. This settings is most useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It provides an ability to use default values for all of them, instead of having to provide individual configuration for each vhost.

This is a global property which is overridden by settings for particular VHost (see :ref:`Add and Manage Domains (VHosts)<addManageDomain>`).

**Available since:** 8.0.0

vhost-message-forward-jid
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** <null>

**Example:** ``'vhost-message-forward-jid' = 'archive@domain.com'``

**Possible values:** 'valid JID'

**Description:** This is a global property for message forwarding for the installation. This property is normally specified on the vhost configuration level, however if you want to forward all messages on your installation and you have many virtual domains this property allows to set message forwarding for all of them. A valid JID must be specified as the forwarding destination. Also a message forwarding plugin must be loaded and activated on the installation for the message forwarding to work.

The null value is used as a default when no configuration is set. This setting is mostly useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It provides the ability to configure a default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This is a global property which is overridden by settings for particular VHost (see :ref:`Add and Manage Domains (VHosts)<addManageDomain>`).

**Available since:** 8.0.0

vhost-presence-forward-jid
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``<null>``

**Example:** ``'vhost-presence-forward-jid' = 'presence-collector@domain.com'``

**Possible values:** valid JID.

**Description:** This is a global property for presence forwarding function for the installation. All user status presences will be forwarded to given XMPP address which can be a component or any other XMPP entity. If the destination entity is a bot connected via c2s connection it probably should be addressed via full JID (with resource part) or the standard XMPP presence processing would refuse to deliver presences from users who are not in the contact list.

This is a global property which is overridden by settings for particular vhost.

The null value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It enables the ability to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-register-enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``true``

**Example:** ``'vhost-register-enabled' = false``

**Possible values:** ``true|false``

**Description:** ``vhost-register-enabled`` is a global property which allows you to switch on/off user registration on the installation. Setting this property to ``false`` does not disable the registration plugin on the server. You can enable registration for selected domains in the domain configuration settings.

This is a global property which is overridden by settings for particular vhost.

The ``true`` value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It allows admins to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-tls-required
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'vhost-tls-required' = true``

**Possible values:** ``true|false``

**Description:** This property is a global settings to switch on/off TLS required mode on the Tigase installation. Setting this property to ``false`` does not turn TLS off. TLS is still available on the server but as an option and this is the client’s decision whether to use encryption or not. If the property is set to true the server will not allow for user authentication or sending any other user data before TLS handshaking is completed.

This is a global property which is overridden by settings for particular vhost.

The ``false`` value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It allows admins to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0
