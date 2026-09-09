package games.mrlaki5.backgammon.Database;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages player profile data, ELO ratings, match history, and cosmetic inventory.
 */
public class PlayerProfileManager {

    private static final String PREFS_NAME = "player_profile_prefs";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_ELO = "player_elo";
    private static final String KEY_WINS = "player_wins";
    private static final String KEY_LOSSES = "player_losses";
    private static final String KEY_TOTAL_GAMES = "player_total_games";
    private static final String KEY_ACTIVE_FRAME = "active_frame";
    private static final String KEY_ACTIVE_DICE = "active_dice";
    private static final String KEY_ACTIVE_TITLE = "active_title";
    public static final String KEY_ACTIVE_THEME = "active_theme";
    private static final String KEY_PURCHASED_ITEMS = "purchased_items";

    public static final int BOT_ELO_EASY = 800;
    public static final int BOT_ELO_MEDIUM = 1100;
    public static final int BOT_ELO_HARD = 1400;
    public static final int BOT_ELO_ROYAL = 1700;

    private final Context context;
    private final SharedPreferences prefs;
    private final DbHelper dbHelper;

    public PlayerProfileManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dbHelper = new DbHelper(this.context);
        initDefaults();
    }

    private void initDefaults() {
        if (!prefs.contains(KEY_ELO)) {
            prefs.edit()
                    .putInt(KEY_ELO, PlayerProfile.DEFAULT_ELO)
                    .putInt(KEY_WINS, 0)
                    .putInt(KEY_LOSSES, 0)
                    .putInt(KEY_TOTAL_GAMES, 0)
                    .putString(KEY_DISPLAY_NAME, "Player 1")
                    .putString(KEY_ACTIVE_FRAME, "frame_default")
                    .putString(KEY_ACTIVE_DICE, "dice_default")
                    .putString(KEY_ACTIVE_TITLE, "title_beginner")
                    .putString(KEY_ACTIVE_THEME, "theme_royal")
                    .apply();

            addPurchasedItem("frame_default");
            addPurchasedItem("dice_default");
            addPurchasedItem("title_beginner");
            addPurchasedItem("theme_royal");
        }
    }

    public static int getBotElo(int difficulty) {
        switch (difficulty) {
            case 0: return BOT_ELO_EASY;
            case 1: return BOT_ELO_MEDIUM;
            case 2: return BOT_ELO_HARD;
            case 3: return BOT_ELO_ROYAL;
            default: return BOT_ELO_MEDIUM;
        }
    }

    public int getElo() {
        return prefs.getInt(KEY_ELO, PlayerProfile.DEFAULT_ELO);
    }

    public int getWins() {
        return prefs.getInt(KEY_WINS, 0);
    }

    /** Returns the total wins ever recorded for unlock requirement checks. */
    public int getTotalWins() {
        int totalWins = prefs.getInt("total_wins", 0);
        return Math.max(totalWins, getWins());
    }

    public int getLosses() {
        return prefs.getInt(KEY_LOSSES, 0);
    }

    public int getTotalGames() {
        return prefs.getInt(KEY_TOTAL_GAMES, 0);
    }

    public int getWinRate() {
        int total = getTotalGames();
        if (total == 0) return 0;
        return (int) Math.round((getWins() * 100.0) / total);
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, "Player 1");
    }

    public void setDisplayName(String name) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply();
    }

    public String getActiveFrame() {
        return prefs.getString(KEY_ACTIVE_FRAME, "frame_default");
    }

    public void setActiveFrame(String frameId) {
        prefs.edit().putString(KEY_ACTIVE_FRAME, frameId).apply();
    }

    public String getActiveDice() {
        return prefs.getString(KEY_ACTIVE_DICE, "dice_default");
    }

    public void setActiveDice(String diceId) {
        prefs.edit().putString(KEY_ACTIVE_DICE, diceId).apply();
    }

    public String getActiveTitle() {
        return prefs.getString(KEY_ACTIVE_TITLE, "title_beginner");
    }

    public void setActiveTitle(String titleId) {
        prefs.edit().putString(KEY_ACTIVE_TITLE, titleId).apply();
    }

    public String getActiveTheme() {
        return prefs.getString(KEY_ACTIVE_THEME, "theme_royal");
    }

    public void setActiveTheme(String themeId) {
        prefs.edit().putString(KEY_ACTIVE_THEME, themeId).apply();
    }

    public Set<String> getPurchasedItems() {
        return prefs.getStringSet(KEY_PURCHASED_ITEMS, new HashSet<>());
    }

    public boolean isItemPurchased(String itemId) {
        Set<String> items = getPurchasedItems();
        return items != null && items.contains(itemId);
    }

    public void addPurchasedItem(String itemId) {
        Set<String> items = new HashSet<>(getPurchasedItems());
        items.add(itemId);
        prefs.edit().putStringSet(KEY_PURCHASED_ITEMS, items).apply();
    }

    /**
     * Records match result, computes new ELO rating via EloCalculator, and saves to database.
     *
     * @param won true if player won
     * @param opponentElo opponent's ELO rating
     * @param gameMode mode of game ("vs_bot", "pass_and_play", "online")
     * @return ELO delta (positive if gained, negative if lost)
     */
    public int recordGameResult(boolean won, int opponentElo, String gameMode) {
        int currentElo = getElo();
        int totalGames = getTotalGames();
        int delta = EloCalculator.ratingDelta(currentElo, opponentElo, won, totalGames);
        int newElo = currentElo + delta;
        if (newElo < 100) newElo = 100;

        int wins = getWins() + (won ? 1 : 0);
        int losses = getLosses() + (won ? 0 : 1);
        int newTotalGames = totalGames + 1;

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_ELO, newElo)
                .putInt(KEY_WINS, wins)
                .putInt(KEY_LOSSES, losses)
                .putInt(KEY_TOTAL_GAMES, newTotalGames);
        if (won) {
            int totalWins = prefs.getInt("total_wins", 0);
            editor.putInt("total_wins", totalWins + 1);
        }
        editor.apply();

        // Update database profile
        try {
            PlayerProfile profile = dbHelper.getOrCreateProfile(getDisplayName());
            profile.setElo(newElo);
            profile.setWins(wins);
            profile.setLosses(losses);
            profile.setTotalGames(newTotalGames);
            dbHelper.saveProfile(profile);
        } catch (Exception ignored) {}

        return delta;
    }
}
