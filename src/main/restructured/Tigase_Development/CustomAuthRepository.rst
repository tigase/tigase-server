Custom ``AuthRepository`` implementation
========================================

``Credentials`` - solid base for authentication
------------------------------------------------

Tigase supports having per-application passwords, allowing having distinct password for each application greatly improving security (see :ref:`application-passwords-admin-guide` for more details). To facilitate it uses ``Credentials``, specified by the namesake interface: ``tigase.auth.credentials.Credentials``, which implementations may store multiple credentials for single account (under ``credentialId``, ie. different credentials for different authentication mechanisms).

It's specifies a couple of essential API methods:
	* ``boolean canLogin();`` - checks if account can perform logging-in
	* ``AccountStatus getAccountStatus();`` - returns account status, i.e. if it's enabled or not
	* ``Entry getEntryForMechanism(String mechanism);`` - find a credential for specified encryption mechanism
	* ``Entry getFirst();`` - returns first available instance of credentials entry
	* ``BareJID getUser();`` - returns bare jid of an account
	* ``boolean isAccountDisabled();`` - checks if account is disabled

that allows handling of Credential entry for particular encryption/authentication mechanism. Essential part of the Credentials API are entries (defined in ``tigase.auth.credentials.Credentials.Entry`` that are actually responsible for performing provided password against defined mechanism.


Simplest ``AuthRepository`` implementation
------------------------------------------

The most basic way to create completely custom authentication is by implementing ``AuthRepository`` interface. While it has a handful or required methods the most important one is ``tigase.db.AuthRepository.getCredentials`` (it has default implementation and you could use ``tigase.db.AuthRepository.getPassword`` but it's deprecated and will be removed).

Credentials allow having multiple, per-application passwords, but in the basic case - using ``PLAIN`` SALS method for authentication, you can use entry implementation just for plain passwords (``tigase.auth.credentials.entries.PlainCredentialsEntry`` class) and wrap it in ``tigase.db.AuthRepository.SingleCredential`` as you would only use single password. It also requires passing parameter indicating whether the account is ``active`` (allowed to login) or ``disabled`` (not allowed to log in).

Here's the basic snippet that will authenticate all authentication attempts if `password` will be used as password:

.. Note::

    Particular password retrieval method is out of scope of this document; see :ref:`using-data-repository` and :ref:`accessing-other-repositories-with-data-repository` for more information on how to use Tigase API for accessing databases.

.. code:: java

    public class SimpleCustomAuthRepository implements AuthRepository {

        @Override
        public Credentials getCredentials(BareJID user, String credentialId) throws TigaseDBException {
            final String passwordFromRepository = "password";
            final PlainCredentialsEntry passwordEntry = new PlainCredentialsEntry(passwordFromRepository);
            return new SingleCredential(user, getAccountStatus(user), passwordEntry);
        }

        @Override
        public AccountStatus getAccountStatus(BareJID user) throws TigaseDBException {
            return AccountStatus.active;
        }
    }

There are also 2 useful methods that are called when user is authenticated or logs out, that may optionally allow you to perform certain additional operations on repository/external services:

.. code:: java

    @Override
    public void loggedIn(BareJID jid) throws TigaseDBException {

    }

    @Override
    public void logout(BareJID user) throws TigaseDBException {

    }

Complete source example is included in the sources as ``tigase.examples.SampleCustomAuthRepository``
