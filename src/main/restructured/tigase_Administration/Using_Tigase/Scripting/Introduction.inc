Scripting Introduction - Hello World!
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This document is the first in a series describing scripting support in the Tigase server showing how to load, install, update and call a script. It contains also an introduction to the scripting API with the first *Hello world!* example.

Since Tigase version 4.3.1 the server supports scripting for administrator commands as well as standard commands.

In theory many different languages can be used to write scripts and the only requirement is that support `JSR-223 <http://www.jcp.org/en/jsr/detail?id=223>`__ for the language is installed. More details can be found on the `Java scripting project site <https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/prog_guide/api.html>`__.

In practice some languages are better supported than others, at the moment we recommend `Groovy <http://groovy-lang.org/>`__. However the following languages are also confirmed to be working: `Scala <http://www.scala-lang.org/>`__, `Python <http://www.python.org/>`__ and `Ruby <http://www.ruby-lang.org/>`__. The `tigase-server GitHub <https://github.com/tigase/tigase-server/blob/master/src/main>`__ contains a few examples for these languages.

.. Note::

   the default Tigase installation contains only libraries for Groovy. Adding support for a different language is as simple as copying a few JAR files to the Tigase ``libs/`` directory.

All the examples presented in this guide are also available as ready to use scripts in the Tigase SVN repository in directory: `src/main/groovy/tigase/admin <https://github.com/tigase/tigase-server/blob/master/src/main/groovy/tigase/admin>`__.

The scripting utilizes only standard XMPP extensions and is by no means specific to any particular solution. We use and prefer Psi client. The whole guide and all the screen-shots are created using Psi client. You can, however, use any other client which supports these extensions as well. As the whole thing is based on the service discovery and ad-hoc commands you need a XMPP client with a good support for both features.

To follow the guide and run all the examples you need will need to have installed Tigase server version 4.3.1 or newer and you have to connect to the server as administrator.

Loading Script at Run Time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

All the scripting stuff is usually based on the service discovery and ad-hoc commands in the Tigase server.

|service disco|

The first thing to do, therefore, is to browse service discovery on the running server. The result you receive will depend on your installation and installed components.

The most interesting things right now are all items with "http://jabber.org/protocol/admin" in their node part. You may have a few scripts loaded already but there are two commands used for scripting management. Their names are descriptive enough: ``New command script`` and ``Remove command script``.

The first is for adding a new script or updating existing and the second is for removing script from the server.

To add a new script you have just to execute ``New command script``. In Psi this is done by double clicking on the element in service discovery list.

|hello1 new script|

The screenshot above shows a couple of options to set for the loaded script:

Description
   is what shows as the script name in the service discovery window. There are no special restrictions on what to put there.

Command id
   is a unique ID of the script (admin command). This is what shows after the "http://jabber.org/protocol/admin" in node part. This needs to be unique or existing script is overwritten.

Language
   a drop down list of all supported scripting languages for your installation. Tigase automatically detects all libraries for scripting languages and lists them here. So all you need is to select the correct language for your script.

Script text
   is just your script content.

When your script is ready and all fields are correctly set, simply press "**Finish**" button and you should receive a message confirming that the script has been loaded successfully.

|loaded ok small|

In this guide we are creating a simple "Hello world" script written in Groovy. What it does is displays a window (ad-hoc command result) with a message: "*Hello admin, how are you?*".

It uses a basic scripting API which is described line by line below:

1. It imports basic Tigase classes.

2. Sets a local variable ``p`` which points to a ``packet`` variable with data received from the client.

3. Creates a ``res`` variable which is response sent back to the client (administrator). The response to the client is of type ``result``. Other possible types will be introduced later.

4. We operate on ad-hoc commands here so the script uses Tigase utility class to set/retrieve command parameters. It sets the window title and a simple message displayed to the user (administrator).

5. The last line returns new packet as a script execution result.

The first, very simple version looks like this:

.. code:: groovy

   import tigase.server.*
   def p = (Packet)packet
   def res = p.commandResult(Command.DataType.result)
   Command.addTitle(res, "Hello World Script")
   Command.addInstructions(res, "Hello admin, how are you?")
   return res

Executing Script
~~~~~~~~~~~~~~~~~~~~~

Once the script is successfully loaded you will have to reload/refresh the service discovery window which now should display one more element on the list.

|service disco with new hello|

As you can see script name is set to what you have entered as "Description" in script loading window - "*Hello world script*". The command node is set to: "http://jabber.org/protocol/admin#hello" if "**hello**" is what is set as the script ID.

To execute the script you just have to double click on the script name (or click execute command if you use any other client).

As a result you should see a simple window similar to the screenshot below displaying our message.

|hello1 result small|

Interaction in Scripts
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Displaying just a message is very nice but is not very useful in most cases. Normally you need to ask the user for some more data or parameters before you can perform any real processing.

Therefore in most cases the administrator script has to display a new window with input fields asking the user for some more data. In this document we present very simple examples, just an introduction so let’s ask about the administrator name before displaying a greeting.

|hello2 asking for name small|

To ask the user for some more information we have to extend example above with some more code:

.. code:: groovy

   import tigase.server.*

   def p = (Packet)packet

   def name = Command.getFieldValue(packet, "name")

   if (name == null) {
     def res = p.commandResult(Command.DataType.form);
     Command.addTitle(res, "Hello World Script")
     Command.addInstructions(res, "Please provide some details")
     Command.addFieldValue(res, "name", name ?: "", "text-single",
       "Your name")
     return res
   }

   def res = p.commandResult(Command.DataType.result)
   Command.addTitle(res, "Hello World Script")
   Command.addInstructions(res, "Hello ${name}, how are you?")

   return res

If you compare both scripts you see that they are quite similar. Before displaying greeting, however, the script tries to retrieve data from the ``name`` input field. If the name had been provided the greeting is displayed, otherwise the script asks for the user name.

|hello2 result small|

Please note, in this case the packet sent back to the user is of type form instead of ``result``. The practical difference is that the type ``result`` displays only **OK** button which when pressed doesn’t send any data to the server. The form packet displays more buttons - **Finish** and **Cancel**. Whichever you press some data is sent back to the server.

This script demonstrates use of two new methods from the utility class "Command": getFieldValue and addFieldValue.

-  The first argument to all Command methods is the packet with ad-hoc command.

-  The second argument is usually the input field name

These two method parameters are actually enough to read the ad-hoc command data. Methods creating input fields in the ad-hoc command need a few arguments more:

-  Next arguments sets a default value displayed to the user. The way to it is set in the example above is specific to Groovy language and is quite useful what will be apparent in later examples.

-  After that we have to specify the field type. All field types are defined in the `XEP-0004 <http://xmpp.org/extensions/xep-0004.html#protocol-fieldtypes>`__ article.

-  The last argument specifies the field label which is displayed to the user.

|hello2 new script|

There are a few other different utility methods in the Command class to set different types of input fields and they will be described in details later on.

To reload the script simply call "New command script" again, enter the script text and make sure you entered exactly the same command ID to replace the old script with the new one.

Or of course, you can enter a new command id to create a new command and make it available on your server.

When the script is loaded on the server, try to execute it. You should get a new dialog window asking for your name as in the screenshot at the beginning of this section. When you have entered your name and clicked the "Finish" button you will see another window with a greeting message along with your name.

Automatic Scripts Loading at Startup Time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The last thing described in this guide is how to automatically load your scripts when the Tigase server starts. The ability to load scripts at run time, update and remove remove them is very useful, especially in emergency cases if something wrong is going on and you want to act without affecting the service.

If you, however have a few dozens scripts you don’t want to manually load them every time the server restarts.

Tigase server automatically loads all scripts at the startup time which are located in the admin scripts directory. Unless you set it differently in the configuration it is: **YourTigaseInstallationDir/scripts/admin/**. All you have to do is to copy all your scripts to this directory and they will be loaded next time the server starts.

But hold on. What about the script parameters: language, description, command id? How are you supposed to set them?

Language is simple. It is detected automatically by the script file extension. So just make sure file extensions are correct and the language is sorted.

The script description and command id needs a little bit more work. You have to include in your script following lines:

::

   AS:Description: The command description
   AS:CommandId: command-id
   AS:Component: comp_name

Please note, there must be at least a single space after the ``AS:Description:`` or ``AS:CommandId:`` string. Everything rest after that, until the end of the line, is treated as either the script description or command id. Put these in your script file and the loader will detect them and set correctly for your script.

.. |service disco| image:: /images/admin/service-disco.png
.. |hello1 new script| image:: /images/admin/hello1-new-script.png
.. |loaded ok small| image:: /images/admin/loaded-ok-small.png
.. |service disco with new hello| image:: /images/admin/service-disco-with-new-hello.png
.. |hello1 result small| image:: /images/admin/hello1-result-small.png
.. |hello2 asking for name small| image:: /images/admin/hello2-asking-for-name-small.png
.. |hello2 result small| image:: /images/admin/hello2-result-small.png
.. |hello2 new script| image:: /images/admin/hello2-new-script.png
