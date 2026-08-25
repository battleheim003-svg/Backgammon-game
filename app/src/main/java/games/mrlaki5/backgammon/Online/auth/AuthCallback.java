package games.mrlaki5.backgammon.Online.auth;

/**
 * Callback for authentication operations.
 */
public interface AuthCallback {
    void onSuccess(OnlineUser user);
    void onError(String errorMessage);
}
