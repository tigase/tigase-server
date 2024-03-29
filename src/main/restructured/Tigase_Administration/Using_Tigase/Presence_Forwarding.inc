.. _Presence-Forwarding:

Presence Forwarding
------------------------

Have you ever thought of displaying your users presence status on the website? Or, maybe, you wanted to integrate XMPP service with your own system and share not only users' accounts but also presence status?

Not only is it possible but also very simple. You have a new option in the domain control form.

Actually there are 2 new options:

1. Presence forward address

2. Message forward address - not fully implemented yet

Presence forward address can be any XMPP address. Usually you want it to be a bot address which can collect your users' presence information. Once this option is set to a valid XMPP address Tigase forwards user’s presence, every time the user changes his status. The presence is processed normally, of course, and distributed to all people from the contact list (roster), plus to this special address. It can be a component or a bot. If this is a bot connecting to a regular XMPP account, **Make sure the presence forward address contains resource part and the bot is connecting with this resource.** Otherwise the presence won’t be delivered to the bot.

|vhost presence forward|

As the screenshot shows, there are new input lines with option for presence forwarding address and message forwarding address. As you can see this option can be specified separately for each domain, so you can have a different forward address for each domain.

If you have your own Tigase installation, the forwarding address can be also set globally and can be the same for all domains. However, for this website, we offer this feature to all our users who have own domains and this can be set on per-domain basis.

Now, the big question. How this can be used? I am attaching below an example code. With just a few lines of code you can connect a command line bot to the server as a client which would collect all presences from users. Code below is a simple Groovy script which receives presence packet and displays them on the console. However, it should be easy enough to store users' presence information in a database and then load it from a web application.

The bot/client uses our `JaXMPP2 <https://github.com/tigase/jaxmpp>`__ library which is included in current builds of Tigase XMPP Server.

You should be able to find a few more code examples on the wiki page.

.. code:: groovy

   package jaxmppexample

   import tigase.jaxmpp.core.client.BareJID
   import tigase.jaxmpp.core.client.SessionObject
   import tigase.jaxmpp.core.client.exceptions.JaxmppException
   import tigase.jaxmpp.core.client.observer.Listener
   import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule
   import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent
   import tigase.jaxmpp.j2se.Jaxmpp

   final Jaxmpp jaxmpp = new Jaxmpp()
   jaxmpp.getProperties().setUserProperty( SessionObject.USER_BARE_JID,
     BareJID.bareJIDInstance( "test4@test.tigase.org" ) )
   jaxmpp.getProperties().setUserProperty(SessionObject.RESOURCE, "presence-collector")
   jaxmpp.getProperties().setUserProperty( SessionObject.PASSWORD, "pass" )
   jaxmpp.getModulesManager().getModule( PresenceModule.class ).addListener(
     PresenceModule.ContactChangedPresence,  new Listener() {
       public void handleEvent( PresenceEvent be ) {
         def msg = (be.getStatus() != null) ? be.getStatus() : "none"
         println( "Presence received:\t" + be.getJid() + " is now " + be.getShow() +
           " (" + msg + ")" )
       }
     }
   )
   println( "Loging in..." )
   jaxmpp.login()
   println( "Waiting for the presence for 10 minutes" )
   Thread.sleep( 10 * 60 * 1000 )
   disconnect()

.. |vhost presence forward| image:: /images/admin/vhost-presence-forward.png