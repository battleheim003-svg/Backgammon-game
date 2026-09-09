package games.mrlaki5.backgammon.Economy;

/**
 * All coin economy constants. Change values here to rebalance.
 */
public final class CoinConfig {
    private CoinConfig() {}

    // === Earning ===
    public static final int WIN_BASE = 10;
    public static final int WIN_BONUS_MEDIUM = 5;
    public static final int WIN_BONUS_HARD = 10;
    public static final int WIN_BONUS_ROYAL = 20;
    public static final int DAILY_CHALLENGE_COMPLETE = 25;
    public static final int STREAK_MILESTONE_EVERY = 5;    // every N consecutive wins
    public static final int STREAK_MILESTONE_REWARD = 15;
    public static final int ACHIEVEMENT_UNLOCK = 50;
    public static final int ACHIEVEMENT_UNLOCK_MAJOR = 100; // games_100, beat_royal
    public static final int FIRST_GAME_OF_DAY = 5;
    public static final int REWARDED_AD_WATCH = 15;
    public static final int DAILY_CHALLENGE_BONUS_CHEST = 25;  // on top of base 25
    public static final int DOUBLE_REWARD_MULTIPLIER = 2;

    // === Spending ===
    public static final int HINT_COST = 20;
    public static final int UNDO_COST = 30;
    public static final int THEME_RENTAL_COST = 50;       // 24-hour rental
    public static final int AVATAR_FRAME_COST = 100;
    public static final int DICE_SKIN_COST = 150;
    public static final int PROFILE_TITLE_COST = 200;

    // === Daily Login Bonus (7-day cycle) ===
    public static final int[] DAILY_LOGIN_REWARDS = {5, 10, 15, 20, 25, 30, 50};
}
