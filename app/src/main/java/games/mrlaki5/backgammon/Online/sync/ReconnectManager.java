package games.mrlaki5.backgammon.Online.sync;

import android.os.Handler;
import android.os.Looper;

import games.mrlaki5.backgammon.Online.NetworkMonitor;

/**
 * Manages reconnection logic for online matches.
 *
 * Rules:
 * - When a player disconnects, the match waits up to RECONNECT_TIMEOUT_SECONDS (60s)
 * - During this window, the other player sees a countdown
 * - If the disconnected player returns within the window, the game resumes
 * - If the timeout expires, the disconnected player forfeits
 *
 * This class coordinates between NetworkMonitor (connectivity detection)
 * and GameSyncService (server communication).
 */
public class ReconnectManager implements NetworkMonitor.Listener {

    public static final int RECONNECT_TIMEOUT_SECONDS = 60;

    public interface ReconnectCallback {
        /** Called when we lose connection — start showing countdown to user. */
        void onDisconnected(int timeoutSeconds);

        /** Called every second with remaining time. */
        void onCountdownTick(int remainingSeconds);

        /** Called when reconnection succeeds. */
        void onReconnected();

        /** Called when reconnection timeout expires — match is forfeited. */
        void onReconnectTimeout();

        /** Called when the opponent disconnects. */
        void onOpponentDisconnected(int timeoutSeconds);

        /** Called when the opponent reconnects. */
        void onOpponentReconnected();
    }

    private final NetworkMonitor networkMonitor;
    private final GameSyncService syncService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ReconnectCallback callback;

    private String matchId;
    private int playerNumber;
    private boolean disconnected = false;
    private int countdown = RECONNECT_TIMEOUT_SECONDS;
    private Runnable countdownRunnable;

    public ReconnectManager(NetworkMonitor networkMonitor, GameSyncService syncService) {
        this.networkMonitor = networkMonitor;
        this.syncService = syncService;
    }

    /**
     * Starts monitoring for disconnection events for the given match.
     */
    public void startMonitoring(String matchId, int playerNumber, ReconnectCallback callback) {
        this.matchId = matchId;
        this.playerNumber = playerNumber;
        this.callback = callback;
        this.disconnected = false;
        networkMonitor.addListener(this);
    }

    /**
     * Stops monitoring. Call when the match ends or the activity is destroyed.
     */
    public void stopMonitoring() {
        networkMonitor.removeListener(this);
        cancelCountdown();
        callback = null;
    }

    @Override
    public void onNetworkAvailable() {
        if (!disconnected) return;
        disconnected = false;
        cancelCountdown();

        // Attempt to reconnect to the match
        if (syncService != null && matchId != null) {
            syncService.connect(matchId, playerNumber, new GameSyncCallback() {
                @Override
                public void onEventReceived(GameEvent event) {
                    // Events will flow through the main sync callback
                }

                @Override
                public void onOpponentDisconnected(int remainingSeconds) {
                    if (callback != null) callback.onOpponentDisconnected(remainingSeconds);
                }

                @Override
                public void onOpponentReconnected() {
                    if (callback != null) callback.onOpponentReconnected();
                }

                @Override
                public void onSyncError(String errorMessage) {
                    // Reconnect failed — treat as timeout
                    if (callback != null) callback.onReconnectTimeout();
                }
            });
        }

        if (callback != null) callback.onReconnected();
    }

    @Override
    public void onNetworkLost() {
        if (disconnected) return;
        disconnected = true;
        countdown = RECONNECT_TIMEOUT_SECONDS;

        if (callback != null) callback.onDisconnected(RECONNECT_TIMEOUT_SECONDS);
        startCountdown();
    }

    /**
     * Called by GameSyncCallback when the opponent disconnects.
     */
    public void handleOpponentDisconnect() {
        if (callback != null) callback.onOpponentDisconnected(RECONNECT_TIMEOUT_SECONDS);
    }

    /**
     * Called by GameSyncCallback when the opponent reconnects.
     */
    public void handleOpponentReconnect() {
        if (callback != null) callback.onOpponentReconnected();
    }

    private void startCountdown() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdown--;
                if (countdown <= 0) {
                    if (callback != null) callback.onReconnectTimeout();
                    return;
                }
                if (callback != null) callback.onCountdownTick(countdown);
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    public boolean isDisconnected() {
        return disconnected;
    }
}
