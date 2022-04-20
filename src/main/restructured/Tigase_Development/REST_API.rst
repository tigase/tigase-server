2. REST API
============

Tigase’s HTTP API component uses the REST module and Groovy scripts responsible for handling and processing incoming HTTP. The end result is Tigase’s REST API. This API may be useful for various integration scenarios.

In these sections we will describe the basic REST endpoints provided by Tigase HTTP API and explain the basics of creating new custom endpoints.

Other endpoints, specific to particular Tigase XMPP Server modules, are described in documentation for the modules providing them. You may also look at ``http://localhost:8080/rest/`` on your local Tigase XMPP Server installation at HTTP API, which will provide you with basic usage examples for REST endpoints available at your installation.

For more informations about configuration of REST module please see section about `??? <#REST module>`__.

2.1. Scripting introduction
----------------------------

Scripts in the HTTP API component are used for processing all of requests.

To add a new action to the HTTP API component, you will need to create a script written in Groovy for which there will be implementation of class extending ``tigase.http.rest.Handler`` class. The URI of script will be created from the file’s location of in the scripts folder. For example, if script ``TestHandler`` with regular expression will be set to ``/test`` and will be placed in ``scripts/rest/tested``, the handler will be called for using the following URI: ``/rest/tested/test``.

2.1.1. Properties
^^^^^^^^^^^^^^^^^^^

If you are extending classes you will need to set following properties:

-  **regex** - Regular expression which is used to match the request URI and parse parameters embedded in the URI. For example: ``/\/([@\/]+)@([@\/]+)/``

-  **requiredRole** - Required role of user in order to be able to access this URI. Available values are: null, "user", and "admin". If ``requiredRole`` is not null, authentication will be required.

-  **isAsync** - If set to true, it will be possible to wait for results, for example waiting for an response IQ stanza.

-  **decodeContent** - If set to false, then content of the request will not be parsed and your script will receive instance of ``HttpServletRequest`` to handle incoming content.

2.1.2. Properties containing closures
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Extended class should also set closures for one or more of following properties: execGet, execPut, execPost, and execDelete depending on which HTTP action or actions you need to support for the URI. **Each closure has a dynamic arguments list**. Below is list of arguments passed to closure which describes how and when the list of arguments changes:

1. **service** - Implementation of Service interface. This is used to access the server database or send/receive XMPP stanzas.

2. **callback** - The ``callback`` closure needs to be called to return data. ``callback`` accepts only one argument of type String,byte[],Map. If data is type of Map it will be encoded to JSON or XML depending of 'Content-Type' header.

3. **user** - Will be passed only if ``requiredRole`` is not null. **In all other cases this argument will not be in arguments list!**

4. **request** - Will be passed only if declared as instance of ``HttpServletRequest`` and it will be instance of ``HttpServletRequest`` of the current HTTP request.

5. **content** - Parsed content of request. This closure will not be in arguments list if Content-Length of request is empty. If Content-Type is XML or JSON returned as Map, otherwise (or if ``decodeContent`` is set to ``false``) it will be an instance of ``HttpServletRequest``.

6. **x** - Additional arguments passed to callback are groups from regular expression matching the URI. **Groups are not passed as a list, but are added to list of arguments as next arguments.**

If property for corresponding HTTP action is not set, then the component will return a 404 HTTP error.


2.1.3. Accessing beans
^^^^^^^^^^^^^^^^^^^^^^^^^^

It is possible to gain access to beans managed by Tigase XMPP Server from within groovy script implementing REST handler. To achieve that implementation of the handler class within groovy script needs to be annotated with ``@Bean`` annotation. In this annotation, you need to pass at least one parameter ``name``, which should contain desired name of the bean under which this handler will be available within the REST module kernel scope.

With that in place, it is possible to use ``@Inject`` annotation on any field of the ``Handler`` implementation class to tell Tigase Kernel to inject instance of a particular class (or instance of class implementing particular interface).

For more details about Tigase Kernel and beans please check ``Tigase Kernel`` section of the Tigase XMPP Server Development Guide.

**Example.**

.. code:: java

   @Bean(name = "test-bean", active = true)
   class TestHandler
           extends tigase.http.rest.Handler {

       @Inject
       private UserRepository userRepo;

       // implementation of the handler...
   }

.. Warning::

    Please remember that your bean is created and registered within the scope of the REST module kernel. So other beans needs to be accessible there for you to access them.

2.2. Usage Examples
--------------------

2.2.1. Retrieving user avatar
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Request using GET method for url /rest/avatar/admin@test-domain.com will return an avatar image for user admin@test-domain.com if an avatar is set in user vCard or will otherwise return a http error 404. Example of full url for avatar of user admin@domain.com

::

   http://localhost:8080/rest/avatar/admin@domain.com

Entering this url in will execute GET request. It may be possible to use the url in your browser.

2.2.2. Retrieving list of available adhoc commands
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Using XML format
~~~~~~~~~~~~~~~~~~~

To retrieve a list of available adhoc commands, make a request using GET method for ``/rest/adhoc/sess-man@domain.com`` where ``sess-man@domain.com`` is jid of component you wish to see commands for. For example, entering the following url: http://localhost:8080/rest/adhoc/sess-man@domain.com in your browser will retrieve a list of all ad-hoc commands available at ``sess-man@domain.com``. This action is protected by authentication done using ``HTTP Basic Authentication``. Valid credentials will be those of users available in user database of this Tigase XMPP Server installation (username in barejid form).

Below is example result of that request:

.. code:: xml

   <items>
     <item>
       <jid>sess-man@domain.com</jid>
       <node>http://jabber.org/protocol/admin#get-active-users</node>
       <name>Get list of active users</name>
     </item>
     <item>
       <jid>sess-man@domain.com</jid>
       <node>del-script</node>
       <name>Remove command script</name>
     </item>
     <item>
       <jid>sess-man@domain.com</jid>
       <node>add-script</node>
       <name>New command script</name>
     </item>
   </items>


Using JSON format
~~~~~~~~~~~~~~~~~~

To retrieve a list of available adhoc commands in JSON, we need to pass ``Content-Type: application/json`` to HTTP header of request or add ``type`` parameter set to ``application/json``. Example result below:

.. code:: json

   {
       "items": [
           {
               "jid": "sess-man@domain.com",
               "node": "http://jabber.org/protocol/admin#get-active-users",
               "name": "Get list of active users"
           },
           {
               "jid": "sess-man@domain.com",
               "node": "del-script",
               "name": "Remove command script"
           },
           {
               "jid": "sess-man@domain.com",
               "node": "add-script",
               "name": "New command script"
           }
       ]
   }


2.2.3. Retrieving command form
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In order to retrieve form with required fields for particular command, you have to sent ``POST`` request with only ``jid`` and ``name`` from the list with all available commands (returned using above command)

Using XML
~~~~~~~~~~~

For example, to get form for adding VHost item make a request using ``POST`` method for ``/rest/adhoc/vhost-man@domain.com`` sending the following content (request requires authentication using Basic HTTP Authentication):

.. code:: xml

   <command>
       <node>comp-repo-item-add</node>
   </command>

Below is example result for request presented above:

.. code:: xml

   <command>
       <jid>vhost-man@domain.com</jid>
       <node>comp-repo-item-add</node>
       <fields>
           <item>
               <var>Domain name</var>
               <value/>
           </item>
           <item>
               <var>Enabled</var>
               <type>boolean</type>
               <value>true</value>
           </item>
           <item>
               <var>Anonymous enabled</var>
               <type>boolean</type>
               <value>true</value>
           </item>
           <item>
               <var>In-band registration</var>
               <type>boolean</type>
               <value>true</value>
           </item>
           <item>
               <var>TLS</var>
               <type>fixed</type>
               <value>This installation forces VHost to require TLS. If you need to use unencrypted connections set &amp;apos;vhost-tls-required&amp;apos;
                   property to &amp;apos;false&amp;apos; in the installation configuration file
               </value>
           </item>
           <item>
               <var>Max users</var>
               <value>0</value>
           </item>
           …
       </fields>
       <instructions>âNOTE: Options without value set will use configuration defined in 'DEFAULT' VHostâ</instructions>
   </command>

Using JSON
~~~~~~~~~~~~~~~

For example, to get form for adding VHost item make a request using ``POST`` method for ``/rest/adhoc/vhost-man@domain.com`` using ``Content-Type: application/json`` and sending the following content (request requires authentication using Basic HTTP Authentication) :

.. code:: json

   {
     "command": {
       "node" : "comp-repo-item-add"
     }
   }

Below is an example result for request presented above:

.. code:: json

   {
     "command": {
       "jid": "vhost-man@domain.com",
       "node": "comp-repo-item-add",
       "fields": [
         {
           "var": "Domain name",
           "value": null
         },
         {
           "var": "Enabled",
           "type": "boolean",
           "value": "true"
         },
         {
           "var": "Anonymous enabled",
           "type": "boolean",
           "value": "true"
         },
         {
           "var": "In-band registration",
           "type": "boolean",
           "value": "true"
         },
         {
           "var": "TLS",
           "type": "fixed",
           "value": "This installation forces VHost to require TLS. If you need to use unencrypted connections set &apos;vhost-tls-required&apos; property to &apos;false&apos; in the installation configuration file"
         },
         {
           "var": "Max users",
           "value": "0"
         }
         …
       ],
       "instructions": "❗NOTE: Options without value set will use configuration defined in 'DEFAULT' VHost❗"
     }
   }

2.2.4. Executing example ad-hoc commands
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Retrieving list of active users
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Using XML
'''''''''''

To execute the command to get a list of active users, make a request using POST method for ``/rest/adhoc/sess-man@domain.com`` sending the following content (request requires authentication using Basic HTTP Authentication):

.. code:: xml

   <command>
     <node>http://jabber.org/protocol/admin#get-active-users</node>
     <fields>
       <item>
         <var>domainjid</var>
         <value>domain.com</value>
       </item>
       <item>
         <var>max_items</var>
         <value>25</value>
       </item>
     </fields>
   </command>

In this request we passed all the parameters needed to execute adhoc command. We passed the node of the adhoc command and values for fields required by that command. We passed values of "domain.com" for "domainjid" field and "25" for "max_items" field. We also need to pass ``Content-Type: text/xml`` to HTTP header of request or add ``type`` parameter set to ``text/xml``.

   **Note**

   In case of multi value fields use following format:

.. code:: xml

   <value>
       <item>first-value</item>
       <item>second-value</item>
   </value>

Below is example result for request presented above:

.. code:: xml

   <command>
     <jid>sess-man@domain.com</jid>
     <node>http://jabber.org/protocol/admin#get-active-users</node>
     <fields>
       <item>
         <var>Users: 2</var>
         <label>text-multi</label>
         <value>admin@domain.com</value>
         <value>user1@domain.com</value>
       </item>
     </fields>
   </command>


Using JSON
'''''''''''

To execute the command to get active users in JSON format, make a request using POST method for /rest/adhoc/sess-man@domain.com sending the following content (this request also requires authentication using Basic HTTP Authentication):

.. code:: json

   {
     "command" : {
       "node" : "http://jabber.org/protocol/admin#get-active-users",
       "fields" : [
         {
           "var" : "domainjid",
           "value" : "domain.com"
         },
         {
           "var" : "max_items",
           "value" : "25"
         }
       ]
     }
   }

In this request we passed all parameters needed to execute adhoc command. We passed the node of adhoc command and values for fields required by adhoc command. In this case we passed value of "domain.com" for "domainjid" field and "25" for "max_items" field.

Below is an example result for request presented above:

.. code:: json

   {
       "command": {
           "jid": "sess-man@domain.com",
           "node": "http://jabber.org/protocol/admin#get-active-users",
           "fields": [
               {
                   "var": "Users: 1",
                   "label": "text-multi",
                   "value": [
                     "admin@domain.com",
                     "user1@domain.com"
                   ]
               }
           ]
       }
   }

Ending a user session
~~~~~~~~~~~~~~~~~~~~~~~

To execute the end user session command, make a request using POST method for ``/rest/adhoc/sess-man@domain.com``. The Context of what is sent, may differ depending on circumstance. For example, it may require authentication using *Basic HTTP Authentication* with admin credentials. *sess-man@domain.com* in URL is the JID of session manager component which usually is in form of *sess-man@domain* where ``domain`` is hosted domain name.

Using XML
''''''''''

To execute the command using XML content you need to set HTTP header ``Content-Type`` to ``application/xml``

.. code:: xml

   <command>
     <node>http://jabber.org/protocol/admin#end-user-session</node>
     <fields>
       <item>
         <var>accountjids</var>
         <value>
           <item>test@domain.com</item>
         </value>
       </item>
     </fields>
   </command>

Where ``test@domain.com`` is JID of user which should be disconnected.

As a result server will return following XML:

.. code:: xml

   <command>
     <jid>sess-man@domain.com</jid>
     <node>http://jabber.org/protocol/admin#end-user-session</node>
     <fields>
       <item>
         <var>Notes</var>
         <type>text-multi</type>
         <value>Operation successful for user test@domain.com/resource</value>
        </item>
     </fields>
   </command>

This will confirm that user ``test@domain.com`` with resource ``resource`` was connected and has been disconnected.

If the user was not connected server will return following response:

.. code:: xml

   <command>
     <jid>sess-man@domain.com</jid>
     <node>http://jabber.org/protocol/admin#end-user-session</node>
     <fields />
   </command>

Using JSON
'''''''''''

To execute the command using JSON you will need to set HTTP header ``Content-Type`` to ``application/json``

.. code:: json

   {
     "command" : {
       "node": "http://jabber.org/protocol/admin#end-user-session",
       "fields": [
           {
               "var" : "accountjids",
               "value" : [
                   "test@domain.com"
               ]
           }
       ]
     }
   }

Where ``test@domain.com`` is JID of user who will be disconnected

As a result, the server will return following JSON:

.. code:: json

   {
     "command" : {
       "jid" : "sess-man@domain.com",
       "node" : "http://jabber.org/protocol/admin#end-user-session",
       "fields" : [
         {
           "var" : "Notes",
           "type" : "text-multi",
           "value" : [
             "Operation successful for user test@domain.com/resource"
           ]
         }
      ]
     }
   }

To confirm that user ``test@domain.com`` with resource ``resource`` was connect and it was disconnected.

If user was not connected server will return the following response:

.. code:: json

   {
     "command" : {
       "jid" : "sess-man@domain.com",
       "node" : "http://jabber.org/protocol/admin#end-user-session",
       "fields" : []
     }
   }

2.2.5. Operations on VHosts/Domains
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


All operations on VHosts are done by making a ``POST`` request to ``/rest/adhoc/vhost-man@domain.com`` (it may require authentication using *Basic HTTP Authentication* with admin credentials). When deciding to use XML or JSON set relevant ``Content-Type`` header.

Adding VHost
~~~~~~~~~~~~~

Adding domain is done using ``comp-repo-item-add`` command sent with all required and desired fields (if something is missing form-to-fill-out will be returned). For the instructions how to retrieve the form/available fields please see `Retrieving command form <#RetrievingCommandForm>`__.

Using XML
''''''''''

To execute the command using XML content you need to set HTTP header ``Content-Type`` to ``application/xml`` and the filled out form (below is trimmed example, see `Retrieving command form <#RetrievingCommandForm>`__ for details how to get complete form):

   **Note**

   It’s essential to include ``command-marker`` in the request, otherwise the form will be returned without adding the VHost.

.. code:: xml

   <command>
       <jid>vhost-man@domain.com</jid>
       <node>comp-repo-item-add</node>
       <fields>
           <item>
               <var>Domain name</var>
               <value>my-new-domain.com</value>
           </item>
           <item>
               <var>Enabled</var>
               <value>true</value>
           </item>
           <item>
               <var>command-marker</var>
               <value>command-marker</value>
           </item>
           …
       </fields>
   </command>

If the domain was added correctly you will receive response with ``Operation successful.`` Note field:

.. code:: xml

   <command>
       <jid>vhost-man@domain.com</jid>
       <node>comp-repo-item-add</node>
       <fields>
           <item>
               <var>Note</var>
               <type>fixed</type>
               <value>Operation successful.</value>
           </item>
       </fields>
   </command>


Using JSON
'''''''''''

To execute the command using XML content you need to set HTTP header ``Content-Type`` to ``application/json`` and the filled out form (below is trimmed example, see `Retrieving command form <#RetrievingCommandForm>`__ for details how to get complete form):

   **Note**

   It’s essential to include ``command-marker`` in the request, otherwise the form will be returned without adding the VHost.

.. code:: json

   {
     "command": {
       "jid": "vhost-man@domain.com",
       "node": "comp-repo-item-add",
       "fields": [
         {
           "var": "Domain name",
           "value": "my-new-awesome-domain.com"
         },
         {
           "var": "Enabled",
           "value": "true"
         },
         {
           "var": "command-marker",
           "value": "command-marker"
         }
         …
       ]
     }
   }

If the domain was added correctly you will receive response with ``Operation successful.`` Note field:

.. code:: json

   {
     "command": {
       "jid": "vhost-man@domain.com",
       "node": "comp-repo-item-add",
       "fields": [
         {
           "var": "Note",
           "type": "fixed",
           "value": "Operation successful."
         }
       ]
     }
   }

Configuring VHost
~~~~~~~~~~~~~~~~~~

Modifying domain configuration is done using ``comp-repo-item-update`` command sent with all required and desired fields (if something is missing form-to-fill-out will be returned). For the instructions how to retrieve the form/available fields please see `Retrieving command form <#RetrievingCommandForm>`__.

Using XML
''''''''''

To execute the command using XML content you need to set HTTP header ``Content-Type`` to ``application/xml`` and the filled out form (below is trimmed example, see `Retrieving command form <#RetrievingCommandForm>`__ for details how to get complete form):

.. Note::

   It’s essential to include ``command-marker`` in the request (otherwise the form will be returned without adding the VHost) and ``item-list`` with value set to the name of the VHost that’s being configured.

.. code:: xml

   <command>
       <jid>vhost-man@domain.com</jid>
       <node>comp-repo-item-update</node>
       <fields>
           <item>
               <var>Domain name</var>
               <value>my-vhost.com</value>
           </item>
           <item>
               <var>Enabled</var>
               <value>true</value>
           </item>
           …
           <item>
               <var>command-marker</var>
               <value>command-marker</value>
           </item>
           <item>
               <var>item-list</var>
               <value>my-vhost.com</value>
           </item>
       </fields>
   </command>

If the domain was added correctly you will receive response with ``Operation successful.`` Note field:

.. code:: xml

   <command>
       <jid>vhost-man@domain.com</jid>
       <node>comp-repo-item-update</node>
       <fields>
           <item>
               <var>Note</var>
               <type>fixed</type>
               <value>Operation successful.</value>
           </item>
       </fields>
   </command>


Using JSON
'''''''''''''

To execute the command using XML content you need to set HTTP header ``Content-Type`` to ``application/json`` and the filled out form (below is trimmed example, see `Retrieving command form <#RetrievingCommandForm>`__ for details how to get complete form):

   **Note**

   It’s essential to include ``command-marker`` in the request (otherwise the form will be returned without adding the VHost) and ``item-list`` with value set to the name of the VHost that’s being configured.

.. code:: json

   {
     "command": {
       "jid": "vhost-man@domain.com",
       "node": "comp-repo-item-update",
       "fields": [
         {
           "var": "Domain name",
           "value": "my-domain.com"
         },
         {
           "var": "Enabled",
           "value": "true"
         },
         …
         {
           "var": "command-marker",
           "value": "command-marker"
         },
         {
           "var": "item-list",
           "value": "my-domain.com"
         }
       ]
     }
   }

If the domain was added correctly you will receive response with ``Operation successful.`` Note field:

.. code:: json

   {
     "command": {
       "jid": "vhost-man@domain.com",
       "node": "comp-repo-item-update",
       "fields": [
         {
           "var": "Note",
           "type": "fixed",
           "value": "Operation successful."
         }
       ]
     }
   }

To confirm that user ``test@domain.com`` with resource ``resource`` was connect and it was disconnected.

If user was not connected server will return the following response:

.. code:: json

   {
     "command" : {
       "jid" : "sess-man@domain.com",
       "node" : "http://jabber.org/protocol/admin#end-user-session",
       "fields" : []
     }
   }


2.2.6. Sending any XMPP Stanza
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

XMPP messages or any other XMPP stanza can be sent using this API by sending an HTTP POST request to (by default) ``http://localhost:8080/rest/stream/?api-key=API_KEY`` with serialized XMPP stanza as a content, where ``API_KEY`` is the API key for HTTP API. This key is set in `etc/config.tdsl <#restModuleConfig>`__. Also, each request needs to be authorized by sending a valid administrator JID and password as user and password of BASIC HTTP authorization method. Content of HTTP request should be encoded in ``UTF-8`` and ``Content-Type`` should be set to ``application/xml``.

Handling of request
~~~~~~~~~~~~~~~~~~~~~~~~

If the sent XMPP stanza does not contain a ``from`` attribute, then the HTTP API component will provide it’s own JID. If ``iq`` stanza is being sent, and no ``from`` attribute is set then the received response will be returned as the content of the HTTP response. Successful requests will return HTTP response code 200.

Examples
~~~~~~~~

**Sending an XMPP message with from set to HTTP API component to full JID.**
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

Data needs to be sent as a HTTP POST request content to ``/rest/stream/?api-key=API_KEY`` URL of the HTTP API component to deliver the message *Example message 1* to *test@example.com/resource-1*.

.. code:: xml

   <message xmlns="jabber:client" type="chat" to="test@example.com/resource-1">
       <body>Example message 1</body>
   </message>

**Sending an XMPP message with ``from`` set to HTTP API component to a bare JID.**
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

Data needs to be sent as a HTTP POST request content to ``/rest/stream/?api-key=API_KEY`` URL of the HTTP API component to deliver message *Example message 2* to *test@example.com*.

.. code:: xml

   <message xmlns="jabber:client" type="chat" to="test@example.com">
       <body>Example message 2</body>
   </message>

**Sending an XMPP message with ``from`` set to specified JID and to a recipients' full JID.**

Data needs to be sent as a HTTP POST request content to ``/rest/stream/?api-key=API_KEY`` URL of the HTTP API component to deliver message *Example message 3* to *test@example.com/resource-1* with sender of message set to *sender@example.com*.

.. code:: xml

   <message xmlns="jabber:client" type="chat" from="sender@example.com" to="test@example.com/resource-1">
       <body>Example message 1</body>
   </message>

2.2.7. Setting XMPP user status
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default XMPP user is visible as unavailable when his client is disconnected. However in some cases we may want to present user a active with some particular presence being set. To control this presence of unavailable XMPP user we can use this feature.

Example contents shown below needs to be sent to (by default) ``http://localhost:8080/rest/user/{user-jid}/status?api-key=API_KEY``, where:

-  ``API_KEY`` is the API key for HTTP API

-  ``{user-jid}`` is a bare jid of the user for which you want to set presence.

.. Tip::

   You may add ``/{resource}`` to the URL after ``/status`` part, where ``{resource}`` is name of the resource for which you want to set presence.

.. Warning::

    You need to add ``'user-status-endpoint@http.{clusterNode}'`` to the list of trusted jids to allow UserStatusEndpoint module to properly integrate with Tigase XMPP Server.

Using XML
~~~~~~~~~~

To set user status you need to set HTTP header ``Content-Type`` to ``application/xml``

.. code:: xml

   <command>
       <available>true</available>
       <priority>-1</priority>
       <show>xa</show>
       <status>On the phone</status>
   </command>

where:

-  ``available`` - may be:

   -  ``true`` - user is available/connected **(default)**

   -  ``false`` - user is unavailable/disconnected

-  ``priority`` - an integer of presence priority. *(It should be always set as a negative value to make sure that messages are not dropped)* **(default: -1)**

-  ``show`` - may be one of ``presence/show`` element values **(optional)**

   -  ``chat``

   -  ``away``

   -  ``xa``

   -  ``dnd``

-  ``status`` - message which should be sent as a presence status message **(optional)**

As a result server will return following XML:

.. code:: xml

   <status>
     <user>test@domain.com/tigase-external</user>
     <available>true</available>
     <priority>priority</priority>
     <show>xa</show>
     <status>On the phone</status>
     <success>true</success>
   </status>

This will confirm that user ``test@domain.com`` with resource ``tigase-external`` has it presence changed (look for ``success`` element value).

Using JSON
~~~~~~~~~~~

To set user status you need to set HTTP header ``Content-Type`` to ``application/json``

.. code:: json

   {
     "available": "true",
     "priority": "-1",
     "show": "xa",
     "status": "On the phone"
   }

where:

-  ``available`` - may be:

   -  ``true`` - user is available/connected **(default)**

   -  ``false`` - user is unavailable/disconnected

-  ``priority`` - an integer of presence priority. *(It should be always set as a negative value to make sure that messages are not dropped)* **(default: -1)**

-  ``show`` - may be one of ``presence/show`` element values **(optional)**

   -  ``chat``

   -  ``away``

   -  ``xa``

   -  ``dnd``

-  ``status`` - message which should be sent as a presence status message **(optional)**

As a result, the server will return following JSON:

.. code:: json

   {
     "status": {
       "user": "test@domain.com/tigase-external",
       "available": "true",
       "priority": "-1",
       "show": "xa",
       "status": "On the phone",
       "success": true
     }
   }

This will confirm that user ``test@domain.com`` with resource ``tigase-external`` has it presence changed (look for ``success`` element value).

2.3. BOSH HTTP Pre-Binding
-----------------------------

2.3.1. Bosh (HTTP) Pre-Binding
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


Binding a user session is done by sending a request using HTTP POST method for ``/rest/adhoc/bosh@domain.com`` with the following content:

   **Note**

   Request requires authentication using Basic HTTP Authentication

.. code:: xml

   <command>
     <node>pre-bind-bosh-session</node>
     <fields>
       <item>
         <var>from</var>
         <value>user_jid@domain/resource</value>
       </item>
       <item>
         <var>hold</var>
         <value>1</value>
       </item>
       <item>
         <var>wait</var>
         <value>60</value>
       </item>
     </fields>
   </command>


2.3.2. Configuration
^^^^^^^^^^^^^^^^^^^^^^^^

The Following parameters can be adjusted:

-  **from** This will be the JID of the user. You may change the ``<value/>`` node of the item identified by the ``from`` variable; this can be either a FullJID or a BareJID. In the latter case, a random resource will be generated for the session being bound.

-  **hold** value. By changing value of ``<value/>`` node of the item identified by ``hold`` variable. This value matches the ``hold`` attribute specified in `XEP-0124: Session Creation Response <http://xmpp.org/extensions/xep-0124.html#session-request>`__

-  **wait** value. By changing value of ``<value/>`` node of the item identified by ``wait`` variable. This value matches the ``wait`` attribute specified in `XEP-0124: Session Creation Response <http://xmpp.org/extensions/xep-0124.html#session-request>`__

As a response one will receive and XML with the result containing additionally available session and RID that can be used in the client to attach to the session, e.g.:

.. code:: xml

   <command>
     <jid>bosh@vhost</jid>
     <node>pre-bind-bosh-session</node>
     <fields>
       <item>
         <var>from</var>
         <label>jid-single</label>
         <value>user_jid@domain/resource</value>
       </item>
       <item>
         <var>hostname</var>
         <label>jid-single</label>
         <value>node_hostname</value>
       </item>
       <item>
         <var>rid</var>
         <label>text-single</label>
         <value>9929332</value>
       </item>
       <item>
         <var>sid</var>
         <label>text-single</label>
         <value>3f1b6e70-8528-44bb-8f23-77e7c4a8cf1a</value>
       </item>
       <item>
         <var>hold</var>
         <label>text-single</label>
         <value>1</value>
       </item>
       <item>
         <var>wait</var>
         <label>text-single</label>
         <value>60</value>
       </item>
     </fields>
   </command>

For example, having the above XML request stored in ``prebind`` file, one can execute the request using ``$curl``:

.. code:: bash

   >curl -X POST -d @prebind http://admin%40domain:pass@domain:8080/rest/adhoc/bosh@domain --header "Content-Type:text/xml"


Using JSON
~~~~~~~~~~~

To execute the command to pre-bind BOSH session in JSON format, make a request using POST method to ``/rest/adhoc/bosh@domain.com`` sending the following content:

.. code:: xml

   {
     "command" : {
       "node" : "pre-bind-bosh-session"",
       "fields" : [
         {
           "var" : "from",
           "value" : "user_jid@domain/resource"
         },
         {
           "var" : "hold",
           "value" : "1"
         },
         {
           "var" : "wait",
           "value" : "60"
         }
       ]
     }
   }

This example replicates the same request presented above in XML format.
