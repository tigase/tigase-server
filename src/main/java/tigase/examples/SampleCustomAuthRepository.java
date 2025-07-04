package tigase.examples;

import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.PlainCredentialsEntry;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.xmpp.jid.BareJID;

import java.time.Duration;
import java.util.Map;

/**
 * This is only and example, sample implementation of AuthRepository and should not be used!
 */
public class SampleCustomAuthRepository implements AuthRepository {

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

    @Override
    public void loggedIn(BareJID jid) throws TigaseDBException {

    }

    @Override
    public void logout(BareJID user) throws TigaseDBException {

    }


    @Override
    public void addUser(BareJID user, String password) throws TigaseDBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPassword(BareJID user) throws TigaseDBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getResourceUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getActiveUsersCountIn(Duration duration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getUsersCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getUsersCount(String domain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean otherAuth(Map<String, Object> authProps) throws TigaseDBException, AuthorizationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void queryAuth(Map<String, Object> authProps) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeUser(BareJID user) throws TigaseDBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAccountStatus(BareJID user, AccountStatus status) throws TigaseDBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updatePassword(BareJID user, String password) throws TigaseDBException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
