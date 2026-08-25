package games.mrlaki5.backgammon.Online.auth;

import java.util.UUID;

/**
 * Stub implementation of AuthProvider for development/testing.
 * Generates a local anonymous user with a random UID.
 * Replace with a real implementation (Firebase, custom backend) in production.
 */
public class StubAuthProvider implements AuthProvider {

    private OnlineUser currentUser;

    @Override
    public void signInAnonymously(AuthCallback callback) {
        String uid = UUID.randomUUID().toString();
        currentUser = new OnlineUser(uid, "Guest_" + uid.substring(0, 6), true);
        callback.onSuccess(currentUser);
    }

    @Override
    public void linkPhoneNumber(String phoneNumber, AuthCallback callback) {
        // Stub: pretend linking succeeded
        if (currentUser != null) {
            currentUser = new OnlineUser(currentUser.getUid(), currentUser.getDisplayName(), false);
            callback.onSuccess(currentUser);
        } else {
            callback.onError("Not signed in");
        }
    }

    @Override
    public void verifySmsCode(String verificationId, String code, AuthCallback callback) {
        // Stub: always succeeds
        if (currentUser != null) {
            callback.onSuccess(currentUser);
        } else {
            callback.onError("Not signed in");
        }
    }

    @Override
    public OnlineUser getCurrentUser() {
        return currentUser;
    }

    @Override
    public void signOut() {
        currentUser = null;
    }

    @Override
    public boolean isSignedIn() {
        return currentUser != null;
    }
}
