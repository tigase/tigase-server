Real-Time Block List (RTBL)
---------------------------------

Real-Time Block List (RTBL) is an answer to spam and abuse. Shared blocklists are commonly used in email to identify sources of spam and are often shared between providers to improve spam detection.

Tigase XMPP Server supports (`XMPP real-time block <https://xmppbl.org/>`__) lists using XMPP Publish-Subscribe protocol (`XEP-0060 <https://xmpp.org/extensions/xep-0060.html>`__). Due to used technology, Tigase XMPP Server with configured RTBL will receive updates from the remotly hosted blocklist immediately, improving reaction time on the possible abuse and decreasing administrator or moderator workload. For more details about XMPP RTBL please visit https://xmppbl.org/.

.. note:: 
   Currently, block lists are used by MUC component, but in the future more functionality will be using block lists to further reduce spam and abuse.

   JIDs blocked are not allowed to:

   * enter any MUC room,
   * send message to MUC room,
   * send private message to MUC room members,

   for rooms hosted on the installation, with the exception to the rooms in which JID is a member of the room.

.. warning::
   This functionality requires working server-to-server (S2S) communication between your default virtual domain on your server and the server hosting XMPP real-time blocklist.

   Without working S2S communication, blocklist may be added but will not receive any entries and will not block anything.

Configuration
^^^^^^^^^^^^^^^^^

Real-time block lists are configured with ad-hoc commands on the `rtbl-component@example.com` XMPP component (`example.com` is the virtual host domain hosted on your installation of Tigase XMPP Server).

If multiple RTBL are configured, then if JID is blocked by any of the block lists, it will be blocked.


Adding RTBL
~~~~~~~~~~~~~~~~~~~~~

To add publicly available RTBL hosted at node `muc_bans_sha256` on `xmppbl.org` we will use Tigase Admin UI. We will select `Add real-time blocklist` (`rtbl-add`) command and then in the provided form we will enter:

* `xmppbl.org` in the `Service address (JID)` field
* `muc_bans_sha256` in the `Node` field
* `SHA-256` in the `Hashing algorithm`

as this list uses `SHA-256` hashing algorithm to report identifiers of blocked JIDs.

.. image:: /images/admin/rtbl-add-form.png
    :width: 600
    :alt: Screenshot of filled form for adding RTBL

After submition of the form, the new blocklist will be added and automatically fetched from the remote server.

Removing RTBL
~~~~~~~~~~~~~~~~~~~~~

To delete RTBL previously added to the installation, we will use `Delete real-time blocklist` (`rtbl-delete`) command and in the provided form we will select blocklist to remove from the list of available blocklists;

.. image:: /images/admin/rtbl-delete-form.png
    :width: 600
    :alt: Screenshot of filled form for removing RTBL

After submission of the form, selected blocklist will be removed and all JIDs blocked by it will be automatically unblocked.

