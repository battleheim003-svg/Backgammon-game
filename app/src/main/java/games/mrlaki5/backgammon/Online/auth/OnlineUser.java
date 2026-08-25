package games.mrlaki5.backgammon.Online.auth;

/**
 * Represents an authenticated online user.
 */
public class OnlineUser {
    private final String uid;
    private final String displayName;
    private final boolean isAnonymous;

    public OnlineUser(String uid, String displayName, boolean isAnonymous) {
        this.uid = uid;
        this.displayName = displayName;
        this.isAnonymous = isAnonymous;
    }

    public String getUid() {
        return uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }
}
