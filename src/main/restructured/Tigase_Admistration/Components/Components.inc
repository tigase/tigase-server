Components
================

The only step is to tell the server what components to load, how to name them and optionally give some extra parameters. To do so open the ``config.tdsl`` file you use in your installation.

Let’s say you want to just add PubSub for now. All you need to do is add the following to the properties file:

.. code::

   pubsub (class: tigase.pubsub.PubSubComponent) {}

Normally, this is not necessary since pubsub is loaded by default, however this is just an example of loading a class with the DSL format.

.. code::

   'pubsub-priv' (class: tigase.pubsub.PubSubComponent) {}

As you can see, we can customize the name of a component in the deceleration, here we are using pubsub-priv.

Although this may be rare, it allows for wide compatibility and platform stability.

Normally, however we want to load few different components like PubSub, MUC, MSN Transport and so on…​. Therefore instead of the above second PubSub we can load the MUC component:

.. code::

   muc (class: tigase.muc.MUCComponent) {}
   pubsub (class: tigase.pubsub.PubSubComponent) {}

Changes to the ``config.tdsl`` file will take effect upon server restart.
