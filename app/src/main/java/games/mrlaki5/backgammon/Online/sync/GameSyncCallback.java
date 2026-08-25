package games.mrlaki5.backgammon.Online.sync;

/**
 * Callback for receiving game events from the remote player.
 */
public interface GameSyncCallback {

    /**
     * Called when a game event is received from the opponent.
     * The client should validate and apply this event to the local game-core state.
     */
    void onEventReceived(GameEvent event);

    /**
     * Called when the opponent disconnects.
     * @param remainingSeconds seconds before the match is forfeited
     */
    void onOpponentDisconnected(int remainingSeconds);

    /**
     * Called when the opponent reconnects after a disconnect.
     */
    void onOpponentReconnected();

    /**
     * Called when a sync error occurs (e.g., server unreachable, invalid state).
     */
    void onSyncError(String errorMessage);
}
