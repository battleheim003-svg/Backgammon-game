package games.mrlaki5.backgammon.Online.matchmaking;

/**
 * Represents a request to find or create a match.
 */
public class MatchRequest {

    public enum Type {
        /** Random matchmaking — find any available opponent. */
        RANDOM,
        /** Create a private room and get an invite code. */
        CREATE_PRIVATE,
        /** Join an existing private room by invite code. */
        JOIN_PRIVATE
    }

    private final Type type;
    private final String playerUid;
    private final String playerName;
    private final String inviteCode; // Only for JOIN_PRIVATE

    private MatchRequest(Type type, String playerUid, String playerName, String inviteCode) {
        this.type = type;
        this.playerUid = playerUid;
        this.playerName = playerName;
        this.inviteCode = inviteCode;
    }

    public static MatchRequest random(String playerUid, String playerName) {
        return new MatchRequest(Type.RANDOM, playerUid, playerName, null);
    }

    public static MatchRequest createPrivate(String playerUid, String playerName) {
        return new MatchRequest(Type.CREATE_PRIVATE, playerUid, playerName, null);
    }

    public static MatchRequest joinPrivate(String playerUid, String playerName, String inviteCode) {
        return new MatchRequest(Type.JOIN_PRIVATE, playerUid, playerName, inviteCode);
    }

    public Type getType() { return type; }
    public String getPlayerUid() { return playerUid; }
    public String getPlayerName() { return playerName; }
    public String getInviteCode() { return inviteCode; }
}
