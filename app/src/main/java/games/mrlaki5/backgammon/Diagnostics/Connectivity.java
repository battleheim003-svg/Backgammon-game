package games.mrlaki5.backgammon.Diagnostics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Cheap online check. The game is offline-first: when there is no connection at all, the ad and
 * analytics SDKs are not even started, so they cannot stall the app waiting for a network.
 */
public final class Connectivity {

    private Connectivity() {}

    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return false;
            }
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Throwable t) {
            // Never let a connectivity check break the game
            return false;
        }
    }
}
