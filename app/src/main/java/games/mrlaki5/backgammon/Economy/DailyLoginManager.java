package games.mrlaki5.backgammon.Economy;

import android.content.Context;
import android.content.SharedPreferences;
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
    private final Context context;

    public DailyLoginManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private long getLastLoginEpochDay() {
        try {
            return prefs.getLong(KEY_LAST_LOGIN_DAY, -1L);
        } catch (ClassCastException e) {
            return -1L;
        }
    }

    /**
     * Returns true if the user can claim today's reward.
     */
    public synchronized boolean canClaimToday() {
        return !isClaimedToday();
    }

    /**
     * Returns current streak day (1..7).
     */
    public synchronized int getCurrentStreak() {
        return getCurrentDay() + 1;
    }

    /**
     * Claims today's daily reward and credits coins. Returns reward amount.
     */
    public synchronized int claimDailyReward() {
        if (hasClaimedToday()) return 0;
        int reward = checkAndGetReward();
        if (reward > 0) {
            prefs.edit().putBoolean(KEY_CLAIMED_TODAY, true).commit();
            CoinManager coinManager = new CoinManager(context);
            coinManager.earn(reward, "daily_login");
            GameAnalytics.get().trackDailyLoginClaimed(getCurrentStreak(), reward);
        }
        return reward;
    }

    /**
     * Call on app open. Returns the reward amount if unclaimed today, or 0 if already claimed.
     */
    public synchronized int checkAndGetReward() {
        long todayEpoch = System.currentTimeMillis() / 86400000L;
        long lastEpoch = getLastLoginEpochDay();
        boolean claimedToday = prefs.getBoolean(KEY_CLAIMED_TODAY, false);

        if (todayEpoch == lastEpoch && claimedToday) {
            return 0; // Already claimed
        }

        int consecutive = prefs.getInt(KEY_CONSECUTIVE_DAYS, 0);

        if (todayEpoch != lastEpoch) {
            long diff = (lastEpoch > 0) ? (todayEpoch - lastEpoch) : -1;
            // Is it the next consecutive day?
            if (diff == 1) {
                consecutive++;
            } else if (diff > 1) {
                consecutive = 0; // Streak broken
            }

            // Cycle resets after 7 days
            if (consecutive >= CoinConfig.DAILY_LOGIN_REWARDS.length) {
                consecutive = 0;
            }

            prefs.edit()
                    .putLong(KEY_LAST_LOGIN_DAY, todayEpoch)
                    .putInt(KEY_CONSECUTIVE_DAYS, consecutive)
                    .putBoolean(KEY_CLAIMED_TODAY, false)
                    .commit();
        }

        return CoinConfig.DAILY_LOGIN_REWARDS[consecutive];
    }

    /**
     * Marks today's reward as claimed. Call after showing the reward animation.
     */
    public synchronized void claimReward() {
        prefs.edit().putBoolean(KEY_CLAIMED_TODAY, true).commit();
    }

    /** Returns current day index in the 7-day cycle (0-6). */
    public synchronized int getCurrentDay() {
        return prefs.getInt(KEY_CONSECUTIVE_DAYS, 0);
    }

    /** Returns true if today's reward has been claimed. */
    public synchronized boolean isClaimedToday() {
        long todayEpoch = System.currentTimeMillis() / 86400000L;
        long lastEpoch = getLastLoginEpochDay();
        return todayEpoch == lastEpoch && prefs.getBoolean(KEY_CLAIMED_TODAY, false);
    }

    public synchronized boolean hasClaimedToday() {
        return isClaimedToday();
    }
}
