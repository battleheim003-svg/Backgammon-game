package games.mrlaki5.backgammon;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks the player's current win streak (consecutive wins vs AI).
 * Resets on loss or quit. Does not count Pass & Play or Tutorial.
 *
 * Stored in SharedPreferences. Lightweight, offline, no backend needed.
 *
 * Retention purpose: motivates "one more game" to keep the streak alive.
 */
public class WinStreakTracker {

    private static final String PREFS_NAME = "win_streak_prefs";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_BEST_STREAK = "best_streak";

    private final SharedPreferences prefs;

    public WinStreakTracker(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Records a win. Increments the streak.
     * @return the new streak value
     */
    public int recordWin() {
        int current = prefs.getInt(KEY_CURRENT_STREAK, 0) + 1;
        int best = prefs.getInt(KEY_BEST_STREAK, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CURRENT_STREAK, current);
        if (current > best) {
            editor.putInt(KEY_BEST_STREAK, current);
        }
        editor.apply();
        return current;
    }

    /**
     * Records a loss. Resets the streak to 0.
     */
    public void recordLoss() {
        prefs.edit().putInt(KEY_CURRENT_STREAK, 0).apply();
    }

    /**
     * Gets the current win streak.
     */
    public int getCurrentStreak() {
        return prefs.getInt(KEY_CURRENT_STREAK, 0);
    }

    /**
     * Gets the best (longest) win streak ever.
     */
    public int getBestStreak() {
        return prefs.getInt(KEY_BEST_STREAK, 0);
    }
}
