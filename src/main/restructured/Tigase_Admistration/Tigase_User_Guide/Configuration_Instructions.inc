Configuration instructions for Psi
-----------------------------------

Psi - Initial configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The first time you run Psi you see a screen like this:

|Psi First Run|

To connect to tigase.org server we need to configure the program. Below are step-by-step instructions for novice users on how to setup Psi.

1. Psi can connect to many Jabber servers at the same time so we have to identify each connection somehow. The first thing to do is assign a name to the connection we just created. As we are going to define connection to tigase.org server let’s just name it: **Tigase**.

   |Psi Add Account|

   **Note!** At the moment you can register an account through the Web site only. This is a single account for both services: The Drupal website and Jabber/XMPP service on the tigase.org domain. If you want to have a Jabber account on the tigase.org server go to the registration page, un-tick "Register new account", and go to the point no 5. You can use guide points 2-4 to register a Jabber account on any other Jabber server.

2. When you press the Add button you will see next window where you can enter your Jabber account details:

   |Psi Empty Account|

3. Invent your user name for the account on Tigase server. Let’s assume your user name is: **frank**. Jabber ID’s however consist of 2 parts - your user name and server address. Exactly the same as an e-mail address. As you are registering an account on tigase.org server, you will have to enter in this field: **frank@tigase.org**. Next enter the password of your choice and click the Register button.

   |Psi Register Account|

4. On successful registration you will receive a confirmation message and you should see a window like this:

   |Register Account Success|

   It may happen that somebody earlier registered an account with the same name you’ve selected for yourself. If so, you will receive error message. You will then have to select another user name and try to register again.

5. After clicking the **OK** button you will see a window with your connection and account setup. You can stick with default values for now.

   |PSI After Registration|

   Just click the **Save** button and this window closes.

6. Now you have your account configured and ready to use but you are still off-line. You can find out whether you are on-line or off-line by looking at the bottom of main Psi window. There you can see **Offline** text.

   Click on this **Offline** text and you will see a list of possible options. Just select **Online**.

   |PSI Connected|

   Now you are connected!

Well, you are now connected but how to talk to other people? How to add friends to the contact list? You can send a message to your friends straight away using the **Psi menu** option **New blank message**. It is much more convenient however, if you could see which of your friends is online and available for chatting and if you could start talking to your friend just by clicking on his name.


Short Instructions How to Add Your First Contact
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

1. Click on Psi menu - the button next to the **Online** text. You will see something like this:

   |PSI Menu|

   From all menu options select the top one - Add a contact:

   |PSI Menu add Contact|

2. The next window will display where you can enter your contact details:

   |PSI Add User Empty|

   You have to know the Jabber ID of the person you want to add to your contact list. Let’s assume, for example, you want to add Tigase server administrator’s Jabber ID to your contact list. So, after you enter these details the window will look like this:

   |PSI Add User Filled|

   Click the **Add** button.

3. Now you will see a confirmation window that a new person has been added to your contact list:

   |PSI Kobit Added|

   But there is more behind the scenes. Adding a contact to your **Roster** (contact list) usually means you can see whether the person is online and available to talk or not. The person however, may not wish you to see his presence. So, to make sure the other person accepts you as a friend Psi sent a request to the address you just entered with the question of whether he agrees to show his presence to you.

   You won’t be able to see the users availability until he sends confirmation.

4. Once the other user sends confirmation back, you will usually receive 2 system events:

   |PSI Kobit Auth Received|

5. Click on the contact to see a window with these messages:

   |PSI Authorized Window|

6. One message just says you have been authorized by the other user:

   |PSI Authorized Window 2|

   So you simply click **Next** to see the second message.

7. The second message is a bit more interesting. It contains the question of whether you also authorize the other user to see your presence. If you want to accept this request just click **Add/Auth**.

   |PSI Authorized Window 3|

8. Finally main Psi window with your new contact:

   |PSI Kobit Added Authorized|

Well done!

You are ready to start Jabbering. Good luck.

Where to go next? For detailed Psi documentation refer to the program Wiki page: http://psi-im.org/wiki/Main_Page

Welcome to the Tigase Administration Guide.

.. |Psi First Run| image:: ../../../asciidoc/admin/images/user/psi-first-run.png
.. |Psi Add Account| image:: ../../../asciidoc/admin/images/user/psi-add-account.png
.. |Psi Empty Account| image:: ../../../asciidoc/admin/images/user/psi-register-account-empty.png
.. |Psi Register Account| image:: ../../../asciidoc/admin/images/user/psi-register-account-nossl.png
.. |Register Account Success| image:: ../../../asciidoc/admin/images/user/psi-register-account-success.png
.. |PSI After Registration| image:: ../../../asciidoc/admin/images/user/psi-after-registration.png
.. |PSI Connected| image:: ../../../asciidoc/admin/images/user/psi-connected.png
.. |PSI Menu| image:: ../../../asciidoc/admin/images/user/psi-menu.png
.. |PSI Menu add Contact| image:: ../../../asciidoc/admin/images/user/psi-menu-add-contact.png
.. |PSI Add User Empty| image:: ../../../asciidoc/admin/images/user/psi-add-user-empty.png
.. |PSI Add User Filled| image:: ../../../asciidoc/admin/images/user/psi-add-user-filled.png
.. |PSI Kobit Added| image:: ../../../asciidoc/admin/images/user/psi-kobit-added.png
.. |PSI Kobit Auth Received| image:: ../../../asciidoc/admin/images/user/psi-kobit-auth-received.png
.. |PSI Authorized Window| image:: ../../../asciidoc/admin/images/user/psi-authorized-window.png
.. |PSI Authorized Window 2| image:: ../../../asciidoc/admin/images/user/psi-authorized-window-2.png
.. |PSI Authorized Window 3| image:: ../../../asciidoc/admin/images/user/psi-authorized-window-3.png
.. |PSI Kobit Added Authorized| image:: ../../../asciidoc/admin/images/user/psi-kobit-added-authorized.png