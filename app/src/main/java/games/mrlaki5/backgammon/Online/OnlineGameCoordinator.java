package games.mrlaki5.backgammon.Online;

import com.royalbackgammon.core.model.GameState;

import games.mrlaki5.backgammon.Online.anticheat.MoveValidator;
import games.mrlaki5.backgammon.Online.auth.AuthProvider;
import games.mrlaki5.backgammon.Online.auth.OnlineUser;
import games.mrlaki5.backgammon.Online.matchmaking.MatchResult;
import games.mrlaki5.backgammon.Online.matchmaking.MatchmakingService;
import games.mrlaki5.backgammon.Online.sync.GameEvent;
import games.mrlaki5.backgammon.Online.sync.GameSyncCallback;
import games.mrlaki5.backgammon.Online.sync.GameSyncService;
import games.mrlaki5.backgammon.Online.sync.ReconnectManager;

/**
 * Coordinates all online multiplayer components for a single match.
 * Acts as the bridge between the game UI (GameActivity) and the
 * network services (auth, matchmaking, sync, reconnect, anti-cheat).
 *
 * Lifecycle:
 * 1. authenticate() — sign in anonymously
 * 2. startMatchmaking() — find or create match
 * 3. onMatchFound() — initialize sync + anti-cheat
 * 4. During game: sendMove() / onOpponentMove()
 * 5. endMatch() — cleanup
 *
 * The actual backend implementation is injected via the service interfaces.
 * This class is backend-agnostic — it works with any AuthProvider,
 * MatchmakingService, and GameSyncService implementation.
 */
public class OnlineGameCoordinator implements GameSyncCallback {

    /**
     * Listener for the UI layer (GameActivity) to respond to online events.
     */
    public interface OnlineGameListener {
        void onAuthenticated(OnlineUser user);
        void onAuthError(String error);
        void onMatchFound(MatchResult result);
        void onMatchmakingWaiting();
        void onRoomCreated(String inviteCode);
        void onOpponentMoveReceived(GameEvent event);
        void onOpponentDiceRoll(GameEvent event);
        void onOpponentNoMoves();
        void onOpponentResigned();
        void onConnectionLost(int timeoutSeconds);
        void onConnectionRestored();
        void onOpponentDisconnected(int timeoutSeconds);
        void onOpponentReconnected();
        void onMatchForfeited(boolean weForfeited);
        void onSyncError(String error);
        void onInvalidMoveDetected(String reason);
    }

    private final AuthProvider authProvider;
    private final MatchmakingService matchmakingService;
    private final GameSyncService syncService;
    private final NetworkMonitor networkMonitor;
    private final ReconnectManager reconnectManager;

    private OnlineGameListener listener;
    private MoveValidator moveValidator;
    private String matchId;
    private int localPlayerNumber;
    private int turnCounter = 0;

    public OnlineGameCoordinator(AuthProvider authProvider,
                                  MatchmakingService matchmakingService,
                                  GameSyncService syncService,
                                  NetworkMonitor networkMonitor) {
        this.authProvider = authProvider;
        this.matchmakingService = matchmakingService;
        this.syncService = syncService;
        this.networkMonitor = networkMonitor;
        this.reconnectManager = new ReconnectManager(networkMonitor, syncService);
    }

    public void setListener(OnlineGameListener listener) {
        this.listener = listener;
    }

    // --- Auth ---

    public void authenticate() {
        if (authProvider.isSignedIn()) {
            if (listener != null) listener.onAuthenticated(authProvider.getCurrentUser());
            return;
        }
        authProvider.signInAnonymously(new games.mrlaki5.backgammon.Online.auth.AuthCallback() {
            @Override
            public void onSuccess(OnlineUser user) {
                if (listener != null) listener.onAuthenticated(user);
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) listener.onAuthError(errorMessage);
            }
        });
    }

    // --- Matchmaking ---

    public void startRandomMatchmaking() {
        OnlineUser user = authProvider.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onAuthError("Not authenticated");
            return;
        }
        matchmakingService.findMatch(
                games.mrlaki5.backgammon.Online.matchmaking.MatchRequest.random(
                        user.getUid(), user.getDisplayName()),
                result -> handleMatchResult(result)
        );
    }

    public void createPrivateRoom() {
        OnlineUser user = authProvider.getCurrentUser();
        if (user == null) return;
        matchmakingService.findMatch(
                games.mrlaki5.backgammon.Online.matchmaking.MatchRequest.createPrivate(
                        user.getUid(), user.getDisplayName()),
                result -> handleMatchResult(result)
        );
    }

    public void joinPrivateRoom(String inviteCode) {
        OnlineUser user = authProvider.getCurrentUser();
        if (user == null) return;
        matchmakingService.findMatch(
                games.mrlaki5.backgammon.Online.matchmaking.MatchRequest.joinPrivate(
                        user.getUid(), user.getDisplayName(), inviteCode),
                result -> handleMatchResult(result)
        );
    }

    public void cancelMatchmaking() {
        matchmakingService.cancelMatchmaking();
    }

    private void handleMatchResult(
            games.mrlaki5.backgammon.Online.matchmaking.MatchResult result) {
        switch (result.getStatus()) {
            case MATCHED:
                matchId = result.getMatchId();
                localPlayerNumber = result.getAssignedPlayer();
                initializeMatch();
                if (listener != null) listener.onMatchFound(result);
                break;
            case WAITING:
                if (listener != null) listener.onMatchmakingWaiting();
                break;
            case ROOM_CREATED:
                matchId = result.getMatchId();
                localPlayerNumber = 1;
                if (listener != null) listener.onRoomCreated(result.getInviteCode());
                break;
            case ERROR:
                if (listener != null) listener.onSyncError(result.getErrorMessage());
                break;
            case CANCELLED:
                break;
        }
    }

    // --- Match lifecycle ---

    private void initializeMatch() {
        // Set up anti-cheat validator with fresh game state
        GameState initialState = GameState.Companion.newGame();
        moveValidator = new MoveValidator(initialState);

        // Connect to game sync
        syncService.connect(matchId, localPlayerNumber, this);

        // Start reconnect monitoring
        reconnectManager.startMonitoring(matchId, localPlayerNumber, reconnectCallback);
        networkMonitor.start();
    }

    /**
     * Called by the local player when they make a move.
     * Sends the event to the server.
     */
    public void sendMove(int from, int to, int dieValue) {
        turnCounter++;
        GameEvent event = GameEvent.move(matchId, turnCounter, localPlayerNumber,
                from, to, dieValue);
        syncService.sendEvent(event);
    }

    /**
     * Called by the local player when they roll dice.
     * In online mode, dice should be server-authoritative.
     * This sends the roll request; the server responds with actual values.
     */
    public void sendDiceRoll(int[] values) {
        turnCounter++;
        GameEvent event = GameEvent.diceRoll(matchId, turnCounter, localPlayerNumber, values);
        syncService.sendEvent(event);
    }

    /**
     * Called when local player has no legal moves.
     */
    public void sendNoMoves() {
        turnCounter++;
        GameEvent event = GameEvent.noMoves(matchId, turnCounter, localPlayerNumber);
        syncService.sendEvent(event);
    }

    /**
     * Called when local player resigns.
     */
    public void resign() {
        syncService.resign(matchId, localPlayerNumber);
        cleanup();
    }

    /**
     * Cleans up all online resources. Call when leaving the game.
     */
    public void cleanup() {
        reconnectManager.stopMonitoring();
        networkMonitor.stop();
        syncService.disconnect();
        matchmakingService.cancelMatchmaking();
    }

    // --- GameSyncCallback ---

    @Override
    public void onEventReceived(GameEvent event) {
        switch (event.getType()) {
            case MOVE:
                // Validate the opponent's move
                MoveValidator.ValidationResult validation = moveValidator.validateMove(event);
                if (validation.isValid()) {
                    moveValidator.applyEvent(event);
                    if (listener != null) listener.onOpponentMoveReceived(event);
                } else {
                    if (listener != null) listener.onInvalidMoveDetected(validation.getReason());
                }
                break;
            case DICE_ROLL:
                MoveValidator.ValidationResult diceValidation =
                        moveValidator.validateDiceRoll(event);
                if (diceValidation.isValid()) {
                    moveValidator.applyEvent(event);
                    if (listener != null) listener.onOpponentDiceRoll(event);
                } else {
                    if (listener != null) listener.onInvalidMoveDetected(
                            diceValidation.getReason());
                }
                break;
            case NO_MOVES:
                moveValidator.applyEvent(event);
                if (listener != null) listener.onOpponentNoMoves();
                break;
            case RESIGN:
                if (listener != null) listener.onOpponentResigned();
                cleanup();
                break;
            case GAME_OVER:
                moveValidator.applyEvent(event);
                break;
            default:
                break;
        }
    }

    @Override
    public void onOpponentDisconnected(int remainingSeconds) {
        reconnectManager.handleOpponentDisconnect();
    }

    @Override
    public void onOpponentReconnected() {
        reconnectManager.handleOpponentReconnect();
    }

    @Override
    public void onSyncError(String errorMessage) {
        if (listener != null) listener.onSyncError(errorMessage);
    }

    // --- ReconnectManager callback (inner instance) ---

    private final ReconnectManager.ReconnectCallback reconnectCallback =
            new ReconnectManager.ReconnectCallback() {
        @Override
        public void onDisconnected(int timeoutSeconds) {
            if (listener != null) listener.onConnectionLost(timeoutSeconds);
        }

        @Override
        public void onCountdownTick(int remainingSeconds) {
            // UI can update countdown display
        }

        @Override
        public void onReconnected() {
            if (listener != null) listener.onConnectionRestored();
        }

        @Override
        public void onReconnectTimeout() {
            if (listener != null) listener.onMatchForfeited(true);
            cleanup();
        }

        @Override
        public void onOpponentDisconnected(int timeoutSeconds) {
            if (listener != null) listener.onOpponentDisconnected(timeoutSeconds);
        }

        @Override
        public void onOpponentReconnected() {
            if (listener != null) listener.onOpponentReconnected();
        }
    };

    // --- Getters ---

    public String getMatchId() { return matchId; }
    public int getLocalPlayerNumber() { return localPlayerNumber; }
    public MoveValidator getMoveValidator() { return moveValidator; }
    public boolean isConnected() { return syncService.isConnected(); }
}
