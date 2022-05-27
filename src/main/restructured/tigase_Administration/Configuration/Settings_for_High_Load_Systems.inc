Linux Settings for High Load Systems
---------------------------------------

There are a few basic settings you have to adjust for high load systems to make sure the server has enough resources to handle a big number of network connections.

The main parameter is a maximum number of opened files allowed for the process to keep at the same time. Each network connection uses a file handler, therefore if the limit is too low you can quickly run out of handlers and the server can not accept any more connections.

This limit is set on 2 levels - on the kernel level (``fs.file-max``) and on the system level (``nofile``).

Another kernel property which can be important in certain configurations (like transports installations or when you use proxy for Bosh connections) is: ``net.ipv4.ip_local_port_range``. This parameter can be set the same way as the ``fs.file-max`` property.

fs.file-max
^^^^^^^^^^^^^^

The ``fs.file-max`` kernel property is set via sysctl command. You can see current settings by executing the command:

.. code:: sh

   # sysctl fs.file-max
   fs.file-max = 358920

If you plan to run high load service with large number of server connections, then this parameter should be at least as twice big as the number of network connections you expect to support. You can change this setting by executing the command:

::

   # sysctl -w fs.file-max=360000
   fs.file-max = 360000

net.ipv4.ip_local_port_range
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can see current settings by executing the command:

.. code:: sh

   # sysctl net.ipv4.ip_local_port_range
   net.ipv4.ip_local_port_range = 32768    61000

You can change this setting by executing the command:

.. code:: sh

   # sysctl -w net.ipv4.ip_local_port_range="1024 65000"
   net.ipv4.ip_local_port_range = 1024 65000

TCP_keepalive
^^^^^^^^^^^^^^

According to `Using TCP keepalive to Detect Network Errors <http://www.gnugk.org/keepalive.html>`__ and `TCP Keepalive HOWTO <https://tldp.org/HOWTO/TCP-Keepalive-HOWTO/usingkeepalive.html>`__ some keepalive settings should be changed to improve reliability - it will enable keep alive functionality (checking if the connection is established and valid) and, by decreasing times and interval - will make detection of broken connections faster.

.. code:: sh

   # sysctl -w net.ipv4.tcp_keepalive_time="60"
   net.ipv4.tcp_keepalive_time = 60
   # sysctl -w net.ipv4.tcp_keepalive_probes="3"
   net.ipv4.tcp_keepalive_probes = 3
   # sysctl -w net.ipv4.tcp_keepalive_intvl="90"
   net.ipv4.tcp_keepalive_intvl = 90
   # sysctl -w net.ipv4.tcp_retries2=4
   net.ipv4.tcp_retries2 = 4


/etc/sysctl.conf
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The above commands let the system remember new settings until the next system restart. If you want to make the change permanent you have to edit the file: ``/etc/sysctl.conf`` and add the property at the end of the file:

.. code:: sh

   fs.file-max=360000
   net.ipv4.ip_local_port_range=1024 65000
   net.ipv4.tcp_keepalive_time=60
   net.ipv4.tcp_keepalive_probes=3
   net.ipv4.tcp_keepalive_intvl=90
   net.ipv4.tcp_retries2=4

It will be automatically loaded next time you start the server.

Command:

.. code:: sh

   # sysctl -p

Causes the ``/etc/systcl.conf`` to be reloaded which is useful when you have added more parameters to the file and don’t want to restart the server.

nofile
^^^^^^^^^^^^^^

This is the property used by the system limits. For example running the command ``ulimit -a`` shows you all limits set for the current user:

.. code:: sh

   # ulimit -a
   core file size          (blocks, -c) 0
   data seg size           (kbytes, -d) unlimited
   file size               (blocks, -f) unlimited
   pending signals                 (-i) 38912
   max locked memory       (kbytes, -l) 32
   max memory size         (kbytes, -m) unlimited
   open files                      (-n) 40960
   pipe size            (512 bytes, -p) 8
   POSIX message queues     (bytes, -q) 819200
   stack size              (kbytes, -s) 8192
   cpu time               (seconds, -t) unlimited
   max user processes              (-u) 38912
   virtual memory          (kbytes, -v) unlimited
   file locks                      (-x) unlimited

To make it even more interesting and more complex, there are 2 types of system limits: **soft limit** which can be temporarily exceeded by the user and **hard limit** which can not be exceeded. To see your **hard limit** execute command:

.. code:: sh

   # ulimit -a -H
   core file size          (blocks, -c) unlimited
   data seg size           (kbytes, -d) unlimited
   file size               (blocks, -f) unlimited
   pending signals                 (-i) 38912
   max locked memory       (kbytes, -l) 32
   max memory size         (kbytes, -m) unlimited
   open files                      (-n) 40960
   pipe size            (512 bytes, -p) 8
   POSIX message queues     (bytes, -q) 819200
   stack size              (kbytes, -s) unlimited
   cpu time               (seconds, -t) unlimited
   max user processes              (-u) 38912
   virtual memory          (kbytes, -v) unlimited
   file locks                      (-x) unlimited

The hard limits are usually bigger then the soft limits or sometimes the same.

For us the most important parameter is: **open files**. You can change the property in file: ``/etc/security/limits.conf``. You have to append 2 following lines to the end of the file:

.. code:: sh

   jabber               soft    nofile         350000
   jabber               hard    nofile         350000

Where the ``jabber`` is the user name of the account running you IM service. You can also set the limits for all users on the machine in a following way:

.. code:: sh

   *               soft    nofile         350000
   *               hard    nofile         350000

For those changes to make an effect you have to logout from the modified account and login again. New limits should be applied.


su and init script
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If one intends to use init scripts for startup purposes (or simply wants to be able to start the server utilizing su command) it’s necessary to adjust PAM configuration by modifying /etc/pam.d/su file and uncomment following line:

.. code:: sh

   session    required   pam_limits.so

Afterwards the init scripts will respect configured limits.