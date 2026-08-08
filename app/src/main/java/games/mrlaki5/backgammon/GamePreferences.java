package games.mrlaki5.backgammon;

import android.content.Context;
import android.content.SharedPreferences;

/** Central preferences for bot strength and visual board theme. */
public final class GamePreferences {
    public static final String FILE_NAME = "Settings";
    public static final String KEY_BOT_DIFFICULTY = "botDifficulty";
    public static final String KEY_BOARD_THEME = "boardTheme";
    public static final String KEY_LANGUAGE = "language";
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_FA = "fa";

    public static final int BOT_EASY = 0;
    public static final int BOT_MEDIUM = 1;
    public static final int BOT_HARD = 2;
    public static final int BOT_ROYAL = 3;

    public static final int THEME_ROYAL = 0;
    public static final int THEME_POP_ART = 1;
    public static final int THEME_CYBERPUNK = 2;
    public static final int THEME_LUXURY = 3;

    private GamePreferences() {}

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static int getBotDifficulty(Context context) {
        return preferences(context).getInt(KEY_BOT_DIFFICULTY, BOT_MEDIUM);
    }

    public static int getBoardTheme(Context context) {
        return preferences(context).getInt(KEY_BOARD_THEME, THEME_ROYAL);
    }

    public static String getLanguage(Context context) {
        return preferences(context).getString(KEY_LANGUAGE, LANGUAGE_EN);
    }

    public static void toggleLanguage(Context context) {
        String nextLanguage=LANGUAGE_EN.equals(getLanguage(context)) ? LANGUAGE_FA : LANGUAGE_EN;
        preferences(context).edit()
                .putString(KEY_LANGUAGE, nextLanguage)
                .apply();
    }

    public static void saveSelections(Context context, int difficulty, int theme) {
        preferences(context).edit()
                .putInt(KEY_BOT_DIFFICULTY, difficulty)
                .putInt(KEY_BOARD_THEME, theme)
                .apply();
    }
}
