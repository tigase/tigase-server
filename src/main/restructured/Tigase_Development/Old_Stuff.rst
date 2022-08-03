Old Stuff
==============

This contains sections on old features, or information pertaining to old builds of Tigase. It is kept here for archival purposes.

Tigase DB Schema Explained
--------------------------------

The schema basics, how it looks like and brief explanation to all rows can be found in the `list of schema files <https://github.com/tigase/tigase-server/tree/master/src/main/database>`__. However, this is hardly enough to understand how it works and how all the data is accessed. There are only 3 basic tables which actually keep all the Tigase server users' data: **tig_users**, **tig_nodes** and **tig_pairs**. Therefore it is not clear at first how Tigase’s data is organized.

Before you can understand the Tigase XMPP Server database schema, how it works and how to use it, is it essential to know what were the goals of it’s development and why it works that way. Let’s start with the API as this gives you the best introduction.

Simplified access can be made through methods:

.. code:: java

   void setData(BareJID user, String key, String value);
   String getData(BareJID user, String key);

And more a complex version:

.. code:: java

   void setData(BareJID user, String subnode, String key, String value);
   String getData(BareJID user, String subnode, String key, String def);

Even though the API contains more methods, the rest is more or less a variation of presented above. A complete API description for all access methods is available in JavaDoc documentation in the `UserRepository <https://github.com/tigase/tigase-server/tree/master/src/main/java/tigase/db/UserRepository.java>`__ interface. So we are not going into too much detail here except for the main idea.

Tigase operates on <*key*, **value**> pairs for the individual user data. The idea behind this was to make the API very simple and also at the same time very flexible, so adding a new plugin or component would not require a database schema change, adding new tables, or conversion of the DB schema to a new version.

As a result the **UserRepository** interface is exposed to all of Tigase’s code, mainly the components and plugins (let’s call all of them modules). These modules simply call set/get methods to store or access module specific data.

As plugins or components are developed independently it may easily happen that developer choses the same key name to store some information. To avoid key name conflicts in the database a 'node' concept has been introduced. Therefore, most modules when set/get key value they also provide a subnode part, which in most cases is just XMLNS or some other unique string.

The 'node' thing is a little bit like directory in a file system, it may contain subnodes which makes the Tigase database behave like a hierarchical structure. And the notation is also similar to file systems, you use just **/** to separate node levels. In practice you can have the database organized like this:

.. code:: sql

   user-name@domain  --> (key, value) pairs
                      |
                  roster -->
                          |
                        item1 --> (key1, value1) pairs.
                          |
                        item2 --> (key1, value1) pairs.

So to access item’s 1 data from the roster you could call method like this:

.. code:: java

   getData("user-name@domain", "roster/item1", key1, def1);

This is huge convenience for the developer, as he can focus on the module logic instead of worrying about data storage implementation and organization. Especially at the prototype phase it speeds development up and allows for a quick experiments with different solutions. In practice, accessing user’s roster in such a way would be highly inefficient so the roster is stored a bit differently but you get the idea. Also there is a more complex API used in some places allowing for more direct access to the database and store data in any format optimized for the scenario.

Right now such a hierarchical structure is implemented on top of SQL databases but initially Tigase’s database was implemented as an XML structure, so it was natural and simple.

In the SQL database we simulate hierarchical structure with three tables:

1. **tig_users** - with main users data, user id (JID), optional password, active flag, creation time and some other basic properties of the account. All of them could be actually stored in tig_pairs but for performance reasons they are in one place to quickly access them with a single, simple query.

2. **tig_nodes** - is a table where the hierarchy is implemented. When Tigase was storing data in XML database the hierarchy was quite complex. However, in a SQL database it resulted in a very slow access to the data and a now more flat structure is used by most components. Please note, every user’s entry has something called root node, which is represented by 'root' string;

3. **tig_pairs** - this is the table where all the user’s information is stored in form of the <key, value> pairs.

So we now know how the data is organized. Now we are going to learn how to access the data directly in the database using SQL queries.

Let’s assume we have a user 'admin@test-d' for whom we want to retrieve the roster. We could simply execute query:

.. code:: sql

   select pval
     from tig_users, tig_pairs
     where user_id = 'admin@test-d' and
           tig_users.uid = tig_pairs.uid and
           pkey = 'roster';

However, if multiple modules store data under the key 'roster' for a single user, we would receive multiple results. To access the correct 'roster' we also have to know the node hierarchy for this particular key. The main users roster is stored under the 'root' node, so the query would look like:

.. code:: sql

   select pval
     from tig_users, tig_nodes, tig_pairs
     where user_id = 'admin@test-d' and
               tig_users.uid = tig_nodes.uid and
               node = 'root' and
               tig_users.uid = tig_pairs.uid and
              pkey = 'roster';

How exactly the information is stored in the **tig_pairs** table depends on the particular module. For the roster it looks a bit like XML content:

.. code:: xml

   <contact jid="all-xmpp-test@test-d" subs="none" preped="simple" name="all-xmpp-test"/>

Why the most recent JDK?
--------------------------

There are many reasons but the main is that we are a small team working on source code. So the whole approach is to make life easier for us, make the project easier to maintain, and development more efficient.

Here is the list:

-  **Easy to maintain** - No third-party libraries are used for the project which makes this project much easier to maintain. This simplifies issues of compatibility between particular versions of libraries. This also unifies coding with a single library package without having to rely on specific versions that may not be supported.

-  **Easy to deploy** - Another reason to not use third-party tools is to make it easier for end-users to install and use the server.

-  **Efficient development** - As no third-party libraries are used, Tigase needs either to implement many things on its own or use as much as possible of JDK functionality. We try to use as much as possible of existing library provided with JDK and the rest is custom coded.

What features of JDKv5 are critical for Tigase development? Why I can’t simply re-implement some code to make it compatible with earlier JDK versions?

-  **Non-blocking I/O for SSL/TLS** - This is functionality which can’t be simply re-implemented in JDK-1.4. As the whole server uses NIO it doesn’t make sense to use blocking I/O for SSL and TLS.

-  **SASL** - This could be re-implemented for JDK-1.4 without much effort.

-  **Concurrent package** - This could be re-implemented for JDK-1.4 but takes a lot of work. This is a critical part of the server as it uses multi-threading and concurrent processing.

-  **Security package** - There number of extensions to the security package which otherwise would not work as easily with earlier versions of JDK.

-  **LinkedHashMap** - in JDKv6 is a basement for the Tigase cache implementation.

-  **Light HTTP server** - JDKv6 offers built-in light HTTP server which is needed to implement HTTP binding (JEP-0124) and HTTP user interface to monitor server activity and work with the server configuration.

As the JDK improves, so does our programming as we gain the ability to use new methods, efficiencies, and sometimes shortcuts.

Currently Tigase requires **JDKv8** and we recommend updating it as often as needed!

API Description for Virtual Domains Management in the Tigase Server
------------------------------------------------------------------------

The purpose of this guide is to introduce vhost management in Tigase server. Please refer to the JavaDoc documentation for all specific details not covered in this guide. All interfaces are well documented and you can use existing implementation as an example code base and reference point. The VHost management files are located in the repository and you can browse them using the `source viewer <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts>`__.

Virtual hosts management in Tigase can be adjusted in many ways through the flexible API. The core elements of the virtual domains management is interface `VHostManager <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostManager.java>`__ class. They are responsible for providing the virtual hosts information to the rest of the Tigase server components. In particular to the `MessageRouter <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/server/MessageRouter.java>`__ class which controls how XMPP packets flow inside the server.

The class you most likely want to re-implement is `VHostJDBCRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostJDBCRepository.java>`__ used as a default virtual hosts storage and implementing the `VHostRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostRepository.java>`__ interface. You might need to have your own implementation in order to store and access virtual hosts in other than Tigase’s own data storage. This is especially important if you are going to modify the virtual domains list through systems other than Tigase.

The very basic virtual hosts storage is provided by `VHostItem <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostItem.java>`__ class. This is read only storage and provides the server a bootstrap vhosts data at the first startup time when the database with virtual hosts is empty or is not accessible. Therefore it is advised that all `VHostItem <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostItem.java>`__ implementations extend this class. The example code is provided in the `VHostJDBCRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostJDBCRepository.java>`__ file.

All components which may need virtual hosts information or want to interact with virtual hosts management subsystem should implement the `VHostListener <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostListener.java>`__ interface. In some cases implementing this interface is necessary to receive packets for processing.

Virtual host information is carried out in 2 forms inside the Tigase server:

1. As a **String** value with the domain name

2. As a `VHostItem <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostItem.java>`__ which contains all the domain information including the domain name, maximum number of users for this domain, whether the domain is enabled or disabled and so on. The JavaDoc documentation contains all the details about all available fields and usage.

Here is a complete list of all interfaces and classes with a brief description for each of them:

1. `VHostManagerIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostManagerIfc.java>`__ - is an interface used to access virtual hosts information in all other server components. There is one default implementation of the interface: `VHostManager <#vhostMgr>`__.

2. `VHostListener <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostListener.java>`__ - is an interface which allows components to interact with the `VHostManager <#vhostMgr>`__. The interaction is in both ways. The VHostManager provides virtual hosts information to components and components provide some control data required to correctly route packets to components.

3. `VHostRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostRepository.java>`__ - is an interface used to store and load virtual domains list from the database or any other storage media. There are 2 implementations for this interface: `VHostConfigRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VhostConfigRepository.java>`__ which loads vhosts information for the configuration file and provides read-only storage and - `VHostJDBCRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostJDBCRepository.java>`__ class which extends `VHostConfigRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VhostConfigRepository.java>`__ and allows for both - reading and saving virtual domains list. VHostJDBCRepository is loaded as a default repository by Tigase server.

4. `VHostItem <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostItem.java>`__ - is an interface which allows for accessing all the virtual domain properties. Sometimes the domain name is not sufficient for data processing. The domain may be temporarily disabled, may have a limited number of users and so on. Instances of this class keep all the information about the domain which might be needed by the server components.

5. `VHostManager <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostManager.java>`__ - the default implementation of the VHostManagerIfc interface. It provides components with the virtual hosts information and manages the virtual hosts list. Processes ad-hoc commands for reloading, updating and removing domains.

6. `VHostConfirRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VhostConfigRepository.java>`__ - a very basic implementation of the `VHostRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostRepository.java>`__ for loading domains list from the configuration file.

7. `VHostJDBCRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostJDBCRepository.java>`__ - the default implementation of the `VHostRepository <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/vhosts/VHostRepository.java>`__ loaded by Tigase server. It allows to read and store virtual domains list in the database accessible through UserRepository.

Extending Virtual Domain settings
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In some cases it is desired to extend Virtual Domain to add some additional settings. Since version 8.1.0 it is possible with use of ``VHostItemExtension`` and VHostItemExtensionProvider`.

To do so, you need to create a class implementing ``VHostItemExtension``. This class will hold values of settings for each virtual host. It is required to make it serializable to ``Element`` and deserializable from ``Element``. Moreover, it is required to make values of this class modifiable by ad-hoc commands.

It is recommended to provide additional methods allowing you to access values of this class.

Additionally, you need to implement ``VHostItemExtensionProvider`` interface as a bean and return a class of your implementation of ``VHostItemExtension``.

*Example VHostItemExtensionProvider implementation for* ``SeeOtherHostVHostItemExtension``.

.. code:: java

   @Bean(name = SeeOtherHostVHostItemExtension.ID, parent = VHostItemExtensionManager.class, active = true)
   public static class SeeOtherHostVHostItemExtensionProvider implements VHostItemExtensionProvider<SeeOtherHostVHostItemExtension> {

       @Override
       public String getId() {
           return SeeOtherHostVHostItemExtension.ID;
       }

       @Override
       public Class<SeeOtherHostVHostItemExtension> getExtensionClazz() {
           return SeeOtherHostVHostItemExtension.class;
       }
   }

Stanza Limitations
---------------------

Although XMPP is robust and can process stanzas of any size in bytes, there are some limitations to keep in mind for Tigase server.

Please keep these in mind when using default Tigase settings and creating custom stanzas.

-  Limit to number of attributes of single element = 50 attributes

-  Limit to number of elements = 1024 elements

-  Limit to length of element name = 1024 characters

-  Limit to length of attribute name = 1024 characters

-  Limit to length of attribute value = 10240 characters

-  Limit to length of content of single element CDATA = 1048576b or 1Mb

These values may be changed.

**Note that these limitations are to elements and attributes that may be within a stanza, but do not limit the overall stanza length.**

Escape Characters
^^^^^^^^^^^^^^^^^^^^^

There are special characters that need to be escaped if they are included in the stanza to avoid conflicts. The rules are similar to normal XML escaping. The following is a list of characters that need to be escaped and what to use to escape them:

::

   &    &amp;
   <    &lt;
   >    &gt;
   "    &quot;
   '    &apos;

API changes in the Tigase Server 5.x
-----------------------------------------

**THIS INFORMATION IS FOR OLDER VERSIONS OF TIGASE**

The API changes can effect you only if you develop own code to run inside Tigase server. The changes are not extensive but in some circumstances may require many simple changes in a few files.

All the changes are related to introducing tigase.xmpp.JID and tigase.xmpp.BareJID classes. It is recommended to use them for all operations performed on the user JID instead of the String class which was used before changes.

There are a few advantages to using the new classes. First of all they do all the user JID checking and parsing, they also perform stringprep processing. Therefore if you use data kept by instance of the JID or BareJID you can be sure they are valid and correct.

These are not all advantages however. JID parsing code appears to use a lot of CPU power to conduct it’s operations. JIDs and parts of the JIDs are used in many places of the stanza processing and the parsing is performed over and over again in all these places, wasting CPU cycles, memory and time. Therefore, great performance benefits can be gained from these new class are in if, once parsed, JIDs are reused in all further stanza processing.

This is where the tigase.server.Packet class comes in handy. Instances of the Packet class encloses XML stanza and pre-parses some, the most commonly used elements of the stanza, stanza source and destination addresses among them. As an effect there are all new methods available in the class:

.. code:: java

   JID getStanzaFrom();
   JID getStanzaTo();
   JID getFrom();
   JID getTo();
   JID getPacketFrom();
   JID getPacketTo();

Whereas following methods are still available but have been deprecated:

.. code:: java

   String getStanzaFrom();
   String getStanzaTo();

Please refer to the JavaDoc documentation for the `Packet <http://docs.tigase.org/tigase-server/snapshot/javadoc/tigase/server/Packet.html>`__ class and methods to learn all the details of these methods and difference between them.

Another difference is that you can no longer create the ``Packet`` instance using a constructor. Instead there are a few factory methods available:

.. code:: java

   static Packet packetInstance(Element elem);
   static Packet packetInstance(Element elem,
       JID stanzaFrom, JID stanzaTo);

Again, please refer to the JavaDoc documentation for all the details. The main point of using these methods is that they actually return an instance of one of the following classes instead of the ``Packet`` class: ``Iq``, ``Presence`` or ``Message``.

There is also a number of utility methods helping with creating a copy of the Packet instance preserving as much pre-parsed data as possible:

.. code:: java

   Packet copyElementOnly();
   Packet errorResult(...);
   Packet okResult(...);
   Packet swapFromTo();
   Packet swapStanzaFromTo();

We try to keep the `JavaDoc <http://docs.tigase.org/tigase-server/snapshot/javadoc/>`__ documentation as complete as possible. Please contact us if you find missing or incorrect information.

The main point is to reuse ``JID`` or ``BareJID`` instances in your code as much as possible. You never know, your code may run in highly loaded systems with throughput of 100k XMPP packets per second.

Another change. This one a bit risky as it is very difficult to find all places where this could be used. There are several utility classes and methods which accept source and destination address of a stanza and produce something. There was a great confusion with them, as in some of them the first was the source address and in others the destination address. All the code has been re-factored to keep the parameter order the same in all places. Right now the policy is: **source address first**. Therefore in all places where there was a method:

.. code:: java

   Packet method(String to, String from);

it has been changed to:

.. code:: java

   Packet method(JID from, JID to);

As far as I know most of these method were used only by myself so I do not expect much trouble for other developers.

