Hack Tigase XMPP Server in Eclipse
====================================

If you want to write code for **Tigase** server we recommend using `Eclipse IDE <//https://eclipse.org/downloads/>`__. Either the IDE for Java or Java EE developers will work.

Requirements
---------------

Eclipse IDE currently requires the use of `Java Development Kit 8 <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`__.

You will also need the M2E plugin for Maven integration, however this can be done inside Eclipse now, so refer to the :ref:`Plugin Installation<m2EPlugin>` section for that.

Installation
--------------

Eclipse does not come as an installer, but rather an archive. Extract the directory to a working location wherever you would like. Now install the JDK software, location is not important as Eclipse will find it automatically.

Before we begin, we will need to clone the repository from git.

Linux
^^^^^^^

For linux operating systems, navigate to a directory where you want the repository to be cloned to and type the following into terminal.

::

   git clone https://repository.tigase.org/git/tigase-server.git

Windows
^^^^^^^^^^^^

Please see the Windows coding guide for instructions on how to obtain source code from git. If you don’t want to install git software specifically, you can use Eclipse’s git plugin to obtain the repository without any new software. First click on File, then Import…​ Next select from Git folder and the Projects from Git

|win git1|

Click next, and now select clone URI

|win git2|

Now click next, and in this window enter the following into the URI field

::

   git://repository.tigase.org/git/tigase-server.git

The rest of the fields will populate automatically

|win git3|

Select the master branch, and any branches you wish to edit. **The master branch should be the only one you need, branches are used for specific code changes**

|win git4|

Now select the directory where you wanted to clone the repository to. This was function as the project root directory you will use later on in the setup.

|win git5|

Once you click next Eclipse will download the repository and any branches you selected to that directory. Note you will be unable to import this git directory since there are no git a project specific files downloaded. However, once downloading is complete you may click cancel, and the git repository will remain in the directory you have chosen.

.. _m2EPlugin:

Setup
---------

Once you have the main window open and have established a workspace (where most of your working files will be stored), click on Help and then Install New Software…​

|Eclipse help|

Under the Work With field enter the following and press enter: http://download.eclipse.org/technology/m2e/releases/

**Note: You may wish to click the Add…​ button and add the above location as a permanent software location to keep the location in memory**

|Eclipse m2Einstall|

You should see the M2 Eclipse software packages show in the main window. Click the check-box and click Next. Once the installer is finished it will need to restart Eclipse.

Once that is done, lets connect Eclipse to the cloned repository.

Click File and Import…​ to bring up the import dialog window. Select Maven and then Existing Maven Project.

|Eclipse importMaven|

Now click Next and point the root directory to where you cloned the git repository, Eclipse should automatically see the pom.xml file and show up in the next window.

|Eclipse importMaven2|

Once the import is finished, you are able to now begin working with Tigase’s code inside Eclipse! Happy coding!

.. |win git1| image:: /images/devguide/win-git1.jpg
.. |win git2| image:: /images/devguide/win-git2.jpg
.. |win git3| image:: /images/devguide/win-git3.jpg
.. |win git4| image:: /images/devguide/win-git4.jpg
.. |win git5| image:: /images/devguide/win-git5.jpg
.. |Eclipse help| image:: /images/devguide/Eclipse-help.jpg
.. |Eclipse m2Einstall| image:: /images/devguide/Eclipse-m2Einstall.jpg
.. |Eclipse importMaven| image:: /images/devguide/Eclipse-importMaven.jpg
.. |Eclipse importMaven2| image:: /images/devguide/Eclipse-importMaven2.jpg