package games.mrlaki5.backgammon.Economy;

import android.content.Context;
import android.content.SharedPreferences;
import games.mrlaki5.backgammon.Util.DateUtil;
import games.mrlaki5.backgammon.Analytics.GameAnalytics;

/**
 * 7-day daily login bonus cycle.
 * Day 1: 5 coins → Day 7: 50 coins (star day).
 * Missing a day resets to Day 1.
 */
public class DailyLoginManager {

    private static final String PREFS_NAME = "daily_login_prefs";
    private static final String KEY_LAST_LOGIN_DAY = "last_login_day";
    private static final String KEY_CONSECUTIVE_DAYS = "consecutive_days";
    private static final String KEY_CLAIMED_TODAY = "claimed_today";

    private final SharedPreferences prefs;

    public DailyLoginManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Call on app open. Returns the reward amount if unclaimed today, or 0 if already claimed.
     */
    public int checkAndGetReward() {
        int today = DateUtil.getDayOfYear();
        int lastLogin = prefs.getInt(KEY_LAST_LOGIN_DAY, -1);
        boolean claimedToday = prefs.getBoolean(KEY_CLAIMED_TODAY, false);

        if (today == lastLogin && claimedToday) {
            return 0; // Already claimed
        }

        int consecutive = prefs.getInt(KEY_CONSECUTIVE_DAYS, 0);

        if (today != lastLogin) {
            // Is it the next consecutive day? (allowing 1-day tolerance via day diff)
            if (lastLogin > 0 && (today - lastLogin) == 1) {
                consecutive++;
            } else if (lastLogin > 0) {
                consecutive = 0; // Streak broken
            }

            // Cycle resets after 7 days
            if (consecutive >= CoinConfig.DAILY_LOGIN_REWARDS.length) {
                consecutive = 0;
            }

            prefs.edit()
                    .putInt(KEY_LAST_LOGIN_DAY, today)
                    .putInt(KEY_CONSECUTIVE_DAYS, consecutive)
                    .putBoolean(KEY_CLAIMED_TODAY, false)
                    .apply();
        }

        return CoinConfig.DAILY_LOGIN_REWARDS[consecutive];
    }

    /**
     * Marks today's reward as claimed. Call after showing the reward animation.
     */
    public void claimReward() {
        prefs.edit().putBoolean(KEY_CLAIMED_TODAY, true).apply();
    }

    /** Returns current day index in the 7-day cycle (0-6). */
    public int getCurrentDay() {
        return prefs.getInt(KEY_CONSECUTIVE_DAYS, 0);
    }

    /** Returns true if today's reward has been claimed. */
    public boolean isClaimedToday() {
        int today = DateUtil.getDayOfYear();
        int lastLogin = prefs.getInt(KEY_LAST_LOGIN_DAY, -1);
        return today == lastLogin && prefs.getBoolean(KEY_CLAIMED_TODAY, false);
    }
}
