package games.mrlaki5.backgammon.Online.matchmaking;

/**
 * Abstraction for the matchmaking system. Implementations can use
 * Firebase Realtime DB, a custom WebSocket server, REST API, etc.
 *
 * Supports three flows:
 * 1. Random matchmaking — enter queue, get matched with any opponent
 * 2. Create private room — get an invite code to share
 * 3. Join private room — enter an invite code to join a friend's room
 */
public interface MatchmakingService {

    /**
     * Starts the matchmaking process based on the request type.
     * Results are delivered asynchronously via the callback.
     *
     * @param request defines type (random/create/join) and player info
     * @param callback receives state updates
     */
    void findMatch(MatchRequest request, MatchmakingCallback callback);

    /**
     * Cancels an active matchmaking request.
     * Removes the player from the queue or destroys the private room.
     */
    void cancelMatchmaking();

    /**
     * Returns true if currently in a matchmaking process.
     */
    boolean isSearching();

    /**
     * Generates a short, human-friendly invite code (e.g., "AB3X7").
     * Used for the "invite friend" flow.
     */
    String generateInviteCode();
}
