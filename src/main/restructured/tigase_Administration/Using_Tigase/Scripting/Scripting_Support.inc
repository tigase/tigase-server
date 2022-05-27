Scripting support in Tigase
--------------------------------

Tigase server supports scripting languages in versions 4.3.1 and higher. These pages describe this feature in details how to create new scripts, upload them to the server, and execute them. The guide also contains API description with code examples.

.. Note::

   Tigase server is known for it very low memory consumption and successfully runs with less then 10MB of RAM memory. However adding scripting support for any non-standard (default) language to Tigase server significantly increases memory requirements for the installation. You cannot expect Tigase server to run on 10MB RAM system if you enabled Python, Scala or any other non-standard language.

.. include:: /tigase_Administration/Using_Tigase/Scripting/Introduction.inc
.. include:: /tigase_Administration/Using_Tigase/Scripting/v4.4.x_Update.inc
.. include:: /tigase_Administration/Using_Tigase/Scripting/Python.inc