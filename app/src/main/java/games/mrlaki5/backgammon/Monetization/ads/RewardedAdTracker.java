package games.mrlaki5.backgammon.Monetization.ads;

import android.content.Context;
import android.content.SharedPreferences;
import games.mrlaki5.backgammon.Util.DateUtil;

/**
 * Tracks rewarded ad usage per placement to enforce frequency caps.
 * Global limit: max 5 rewarded ads per day across ALL placements.
 * Global cooldown: 60 seconds between any two rewarded ads.
 */
public class RewardedAdTracker {

    private static final String PREFS_NAME = "rewarded_ad_tracker_prefs";
    private static final String KEY_TOTAL_TODAY = "total_today";
    private static final String KEY_LAST_AD_TIME = "last_ad_time";
    private static final String KEY_LAST_DAY = "last_day";
    private static final int MAX_TOTAL_PER_DAY = 5;
    private static final int GLOBAL_COOLDOWN_MS = 60_000;

    private final SharedPreferences prefs;

    public RewardedAdTracker(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        refreshIfNewDay();
    }

    private void refreshIfNewDay() {
        int today = DateUtil.getDayOfYear();
        if (today != prefs.getInt(KEY_LAST_DAY, -1)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_LAST_DAY, today);
            editor.putInt(KEY_TOTAL_TODAY, 0);
            for (RewardedAdPlacement p : RewardedAdPlacement.values()) {
                editor.putInt("count_" + p.id, 0);
            }
            editor.apply();
        }
    }

    /**
     * Returns true if the given placement can show a rewarded ad right now.
     */
    public boolean canShow(RewardedAdPlacement placement) {
        refreshIfNewDay();

        // Global daily limit
        if (prefs.getInt(KEY_TOTAL_TODAY, 0) >= MAX_TOTAL_PER_DAY) return false;

        // Global cooldown
        long lastTime = prefs.getLong(KEY_LAST_AD_TIME, 0);
        if (System.currentTimeMillis() - lastTime < GLOBAL_COOLDOWN_MS) return false;

        // Per-placement daily limit
        if (placement.maxPerDay > 0) {
            int count = prefs.getInt("count_" + placement.id, 0);
            if (count >= placement.maxPerDay) return false;
        }

        // Per-placement cooldown
        if (placement.cooldownSeconds > 0) {
            long lastPlacement = prefs.getLong("last_" + placement.id, 0);
            if (System.currentTimeMillis() - lastPlacement < placement.cooldownSeconds * 1000L) return false;
        }

        return true;
    }

    /**
     * Records that a rewarded ad was shown for the given placement.
     * Call AFTER the ad is successfully shown and reward granted.
     */
    public void recordShow(RewardedAdPlacement placement) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putInt(KEY_TOTAL_TODAY, prefs.getInt(KEY_TOTAL_TODAY, 0) + 1)
                .putLong(KEY_LAST_AD_TIME, now)
                .putInt("count_" + placement.id, prefs.getInt("count_" + placement.id, 0) + 1)
                .putLong("last_" + placement.id, now)
                .apply();
    }

    /** Returns how many times the placement was used today. */
    public int getUsageToday(RewardedAdPlacement placement) {
        refreshIfNewDay();
        return prefs.getInt("count_" + placement.id, 0);
    }

    /** Returns remaining global daily rewarded ads. */
    public int getRemainingToday() {
        refreshIfNewDay();
        return MAX_TOTAL_PER_DAY - prefs.getInt(KEY_TOTAL_TODAY, 0);
    }
}
