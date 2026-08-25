package games.mrlaki5.backgammon.Online.sync;

/**
 * Abstraction for real-time game state synchronization between two clients.
 * Implementations can use:
 * - Firebase Realtime Database (listeners on match document)
 * - WebSocket (Socket.IO / raw WS to a custom server)
 * - REST polling (fallback for restricted networks)
 *
 * Events flow: client sends event → server validates → broadcasts to both clients.
 * The server persists the match state for reconnection support.
 */
public interface GameSyncService {

    /**
     * Connects to an active match and starts listening for opponent events.
     *
     * @param matchId the unique match identifier from matchmaking
     * @param playerNumber this client's player number (1 or 2)
     * @param callback receives opponent's events
     */
    void connect(String matchId, int playerNumber, GameSyncCallback callback);

    /**
     * Sends a game event to the server/opponent.
     * The event is validated server-side before being forwarded.
     *
     * @param event the game event to send
     */
    void sendEvent(GameEvent event);

    /**
     * Sends the full serialized game state to the server.
     * Used for: initial state persistence, reconnection sync, anti-cheat verification.
     *
     * @param matchId the match identifier
     * @param gameStateJson the full game state as JSON (from game-core's GameState.toJson())
     */
    void sendFullState(String matchId, String gameStateJson);

    /**
     * Requests the full game state from the server (for reconnection).
     * The response is delivered via the callback as a series of events
     * that replay the game from the beginning.
     *
     * @param matchId the match identifier
     * @param callback receives replayed events
     */
    void requestFullState(String matchId, GameSyncCallback callback);

    /**
     * Disconnects from the current match.
     * Does NOT forfeit — the server keeps the match alive for reconnection.
     */
    void disconnect();

    /**
     * Signals that this player is resigning/leaving permanently.
     * The server marks the match as over.
     */
    void resign(String matchId, int playerNumber);

    /**
     * Returns true if currently connected to a match.
     */
    boolean isConnected();
}
