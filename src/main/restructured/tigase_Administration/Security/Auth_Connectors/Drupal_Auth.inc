Drupal Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^

Currently, we can only check authentication against a **Drupal** database at the moment. Full **Drupal** authentication is not implemented as of yet.

As **Drupal** keeps encrypted passwords in database the only possible authorization protocols are those based on PLAIN passwords.

To protect your passwords **Tigase** server must be used with SSL or TLS encryption.

Implementation of a **Drupal** database based authorization is located in ``tigase.db.jdbc.DrupalAuth`` class. Although this class is capable of adding new users to the repository I recommend to switch in-band registration off due to the caching problems in **Drupal.** Changes in database are not synchronized with **Drupal** yet. Functionality for adding new users is implemented only to ease user accounts migration from different repository types from earlier **Tigase** server installations.

The purpose of that implementation was to allow all accounts administration tasks from **Drupal** like: account creation, all accounts settings, like e-mail, full name, password changes and so on.

**Tigase** server uses following fields from **Drupal** database: name (user account name), pass (user account password), status (status of the account). Server picks up all changes instantly. If user status is not 1 then server won’t allow user to login trough XMPP even if user provides valid password.

There is no *Roster* management in **Drupal** yet. So Roster management have to be done from the XMPP client.