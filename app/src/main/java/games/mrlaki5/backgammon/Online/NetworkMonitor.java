package games.mrlaki5.backgammon.Online;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observes network connectivity state and notifies listeners.
 * Used to show "connection lost" banners and pause/resume online games.
 */
public class NetworkMonitor {

    public interface Listener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile boolean connected = true;

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager)
                this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.connected = checkCurrentConnectivity();
    }

    /**
     * Starts monitoring network state changes.
     */
    public void start() {
        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (!connected) {
                    connected = true;
                    mainHandler.post(() -> {
                        for (Listener l : listeners) {
                            l.onNetworkAvailable();
                        }
                    });
                }
            }

            @Override
            public void onLost(Network network) {
                // Double-check — onLost fires per-network, not globally
                if (!checkCurrentConnectivity()) {
                    connected = false;
                    mainHandler.post(() -> {
                        for (Listener l : listeners) {
                            l.onNetworkLost();
                        }
                    });
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    /**
     * Stops monitoring. Call in onDestroy or when online mode ends.
     */
    public void stop() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns true if the device currently has internet connectivity.
     */
    public boolean isConnected() {
        return connected;
    }

    private boolean checkCurrentConnectivity() {
        if (connectivityManager == null) return false;
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
