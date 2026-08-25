package games.mrlaki5.backgammon.Online.auth;

/**
 * Abstraction for authentication. Implementations can use Firebase Auth,
 * a custom backend with JWT, or any other auth provider.
 *
 * Default behavior: anonymous (guest) login with no friction.
 * Optional: link to phone number for account persistence.
 */
public interface AuthProvider {

    /**
     * Signs in anonymously (guest mode). This is the default path
     * to minimize friction for new users in the Iranian market.
     */
    void signInAnonymously(AuthCallback callback);

    /**
     * Links the current anonymous account to a phone number.
     * Used for account recovery and persistent leaderboard identity.
     *
     * @param phoneNumber E.164 format phone number
     * @param callback result callback
     */
    void linkPhoneNumber(String phoneNumber, AuthCallback callback);

    /**
     * Verifies an SMS code sent during phone linking.
     */
    void verifySmsCode(String verificationId, String code, AuthCallback callback);

    /**
     * Returns the currently signed-in user, or null if not authenticated.
     */
    OnlineUser getCurrentUser();

    /**
     * Signs out the current user.
     */
    void signOut();

    /**
     * Returns true if a user is currently signed in (anonymous or identified).
     */
    boolean isSignedIn();
}
