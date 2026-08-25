package games.mrlaki5.backgammon.Online.matchmaking;

/**
 * Callback for matchmaking state changes.
 */
public interface MatchmakingCallback {
    /**
     * Called when the matchmaking state changes (waiting, matched, error, etc.)
     */
    void onMatchmakingUpdate(MatchResult result);
}
