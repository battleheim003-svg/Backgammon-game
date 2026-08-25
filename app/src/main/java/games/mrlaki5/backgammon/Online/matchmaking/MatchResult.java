package games.mrlaki5.backgammon.Online.matchmaking;

/**
 * Result of a matchmaking operation.
 */
public class MatchResult {

    public enum Status {
        /** Match found — both players ready. */
        MATCHED,
        /** Waiting in queue for an opponent. */
        WAITING,
        /** Private room created — share the invite code. */
        ROOM_CREATED,
        /** An error occurred. */
        ERROR,
        /** Matchmaking was cancelled. */
        CANCELLED
    }

    private final Status status;
    private final String matchId;
    private final String inviteCode;
    private final String opponentName;
    private final String opponentUid;
    private final int assignedPlayer; // 1 or 2 — which player number this client is
    private final String errorMessage;

    private MatchResult(Status status, String matchId, String inviteCode,
                        String opponentName, String opponentUid,
                        int assignedPlayer, String errorMessage) {
        this.status = status;
        this.matchId = matchId;
        this.inviteCode = inviteCode;
        this.opponentName = opponentName;
        this.opponentUid = opponentUid;
        this.assignedPlayer = assignedPlayer;
        this.errorMessage = errorMessage;
    }

    public static MatchResult matched(String matchId, String opponentName,
                                       String opponentUid, int assignedPlayer) {
        return new MatchResult(Status.MATCHED, matchId, null,
                opponentName, opponentUid, assignedPlayer, null);
    }

    public static MatchResult waiting() {
        return new MatchResult(Status.WAITING, null, null, null, null, 0, null);
    }

    public static MatchResult roomCreated(String matchId, String inviteCode) {
        return new MatchResult(Status.ROOM_CREATED, matchId, inviteCode, null, null, 1, null);
    }

    public static MatchResult error(String message) {
        return new MatchResult(Status.ERROR, null, null, null, null, 0, message);
    }

    public static MatchResult cancelled() {
        return new MatchResult(Status.CANCELLED, null, null, null, null, 0, null);
    }

    public Status getStatus() { return status; }
    public String getMatchId() { return matchId; }
    public String getInviteCode() { return inviteCode; }
    public String getOpponentName() { return opponentName; }
    public String getOpponentUid() { return opponentUid; }
    public int getAssignedPlayer() { return assignedPlayer; }
    public String getErrorMessage() { return errorMessage; }
}
