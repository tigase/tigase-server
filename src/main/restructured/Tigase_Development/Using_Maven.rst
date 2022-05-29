.. _usingMaven:

Using Maven
================

Documents Describing Maven Use with the Tigase Projects

Setting up Maven in Windows
--------------------------------

Here at Tigase, we employ Apache Maven to download latest builds, compile codes for export, and check for errors in the code during build. This guide will go over installing and running Maven from a Windows operating environment. We will consider windows versions 7, 8, and 8.1 for this guide. Because Maven does not come with an installer, there is a manual install process which might be a bit daunting for the new user, but setup and use is fairly simple.

Requirements
^^^^^^^^^^^^^^^^^

1. Maven requires Java Development Kit (JDK) 6 or later. As Tigase requires the latest JDK to run, that will do for our purposes. If you haven’t installed it yet, download the installer from `this website <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`__. Once you install JDK and restart your machine, be sure that you have the **JAVA_HOME** variable entered into Environment Variables so calls to Java will work from the command line.

2. Download the Maven package from `here <https://maven.apache.org/download.cgi>`__ and unpack it into a directory of your choice. For this guide we will use ``C:\Maven\`` .

Setting up Environment Variables
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The Environment Variables panel is brought up from the Control Panel by clicking **System and Security** > **System** > **Advanced System Settings**. Now click the |Environment Variables| button at the bottom of the panel and the Environment Variables panel will show.

**IMPORTANT NOTICE: CHANGING THESE SETTINGS CAN BREAK OTHER FUNCTIONS IN THE OPERATING SYSTEM. DO NOT FOLLOW THIS GUIDE IF YOU DO NOT KNOW WHAT YOU ARE DOING!**

|Env Panel|

We need to first add two variable paths to the System variables to account for Maven’s install location. As there are some programs that look for M2_HOME, and others that look for MAVEN_HOME, it’s easier to just add both and have all the bases covered.

Click on New…​

|Env New|

For the Name, use M2_HOME, and for the variable enter the path to maven, which in this case is

::

   C:\Maven

Create another new variable with the MAVEN_HOME name and add the same directory. **These variable values just point to where you have unpacked maven, so they do not have to be in the C directory.**

Go down to the system variables dialog and select Path, then click on Edit. The Path variables are separated by semicolons, find the end of the Variable value string, and add the following after the last entry:

::

   ;%M2_HOME%\bin;%MAVEN_HOME%\bin;

We have added two variables using the %% wildcards surrounding our Variable names from earlier.

Testing Maven
^^^^^^^^^^^^^^^^^^^

Now we must test the command line to be sure everything installed correctly. Bring up the command line either by typing ``cmd`` in search, or navigating the start menu.

From the prompt, you do not need to change directory as setting Path allows you to reference it. Type the following command: ``mvn -v``

something like this should show up

::

   Apache Maven 3.3.3 (7994120775791599e205a5524ec3e0dfe41d4a06; 2015-04-22T04:57:3
   7-07:00)
   Maven home: C:\Maven
   Java version: 1.8.0_45, vendor: Oracle Corporation
   Java home: C:\Program Files\Java\jdk1.8.0_45\jre
   Default locale: en_US, platform encoding: Cp1252
   OS name: "windows 7", version: "6.1", arch: "amd64", family: "dos"

If you see this message, success! You have finished installation and are ready to use Maven! If not, go back on your settings and insure that JDK is installed, and the JAVA_HOME, M2_HOME, and MAVEN_HOME variables are set properly.

.. |Environment Variables| image:: /images/devguide/images/Env-button.jpg
.. |Env Panel| image:: /images/devguide/Env-Panel.jpg
.. |Env New| image:: ./images/devguide/Env-New.jpg

A Very Short Maven Guide
------------------------------

If you don’t use `Maven <http://maven.apache.org/>`__ at all or use it once a year you may find the document a useful maven commands reminder:

Snapshot Compilation and Snapshot Package Generation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  ``mvn compile`` - compilation of the snapshot package

-  ``mvn package`` - create snapshot jar file

-  ``mvn install`` - install in local repository snapshot jar file

-  ``mvn deploy`` - deploy to the remote repository snapshot jar file

Release Compilation, Generation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  ``mvn release:prepare`` prepare the project for a new version release

-  ``mvn release:perform`` execute new version release generation

Generating tar.gz, tar.bz2 File With Sources Only
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  ``mvn -DdescriptorId=src assembly:assembly``

Any of these commands will work when your commandline is in a directory with a pom.xml file. This file will instruct what Maven will do.

Profiles
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Maven uses profiles with the -P switch to tell what to compile and build. Tigase uses two different profiles:

-  -Pdist - creates distribution archives

-  -Pdoc - creates documentation