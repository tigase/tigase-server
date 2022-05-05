Server Compilation
====================

Tigase XMPP Server Project uses Maven for compilation. For details on Maven and it’s use, please see the `Maven Guide. <#usingMaven>`__

Distribution Packages
-----------------------

Once Compiled, Tigase creates two separate distribution archives:

-  **-dist** is a minimal version containing only tigase-server, tigase-xmltools and tigase-utils, MUC, Pubsub, and HTTP.

-  **-dist-max** is a version containing all additional tigase components as well as dependencies required by those components.

They will be available as both zip and tarball.

Building Server and Generating Packages
-------------------------------------------

Server binary and it’s documentation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After cloning tigase-server repository:

.. code:: bash

   git clone https://repository.tigase.org/git/tigase-server.git
   cd tigase-server

You compile server with maven :

.. code:: bash

   mvn clean install

This will: - Build Tigase XMPP tigase-server jar in tigase-server/target.

If you wish to include compilation of the documentation use *distribution* profile:

.. code:: bash

   mvn -Pdist clean install

This will - compile server binaries. - generate javadoc and manual documentation ``tigase-server/target/_docs`` directory.

Server distribution packages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Distribution building is handled by separate project (`Tigase Server Distribution <https://github.com/tigase/tigase-server-distribution>`__)

In order to build distribution packages \* clone tigase-server-distribution repository:

.. code:: bash

   git clone https://git.tigase.tech/tigase-server-distribution
   tigase-server-distribution

and compile it using maven with *distribution* profile:

.. code:: bash

   mvn -Pdist clean install

This will:

-  compile all documentation sources (including dependencies) and place them in ``tigase-server-distribution/target/_docs`` directory

-  download all dependencies in defined versions and put them in ``tigase-server-distribution/target/dist/jars/`` directory.

-  create both types of distribution packages (-dist and -dist-max) and place them in ``tigase-server-distribution/target/_dist/`` directory.

Running Server
-------------------

Afterwards you can run the server with the regular shell script from within ``server`` module:

.. code:: bash

   cd server
   ./scripts/tigase.sh start etc/tigase.conf

Please bear in mind, that you need to provide correct setup in etc/config.tdsl configuration files for the server to work correctly.