package games.mrlaki5.backgammon.GameView.themes;

import games.mrlaki5.backgammon.GamePreferences;

/**
 * Factory providing BoardTheme instances corresponding to theme IDs.
 */
public class BoardThemeFactory {

    private static final BoardTheme ROYAL = new RoyalTheme();
    private static final BoardTheme POP_ART = new PopArtTheme();
    private static final BoardTheme CYBERPUNK = new CyberpunkTheme();
    private static final BoardTheme LUXURY = new LuxuryTheme();

    public static BoardTheme getTheme(int themeId) {
        switch (themeId) {
            case GamePreferences.THEME_POP_ART:
                return POP_ART;
            case GamePreferences.THEME_CYBERPUNK:
                return CYBERPUNK;
            case GamePreferences.THEME_LUXURY:
                return LUXURY;
            case GamePreferences.THEME_ROYAL:
            default:
                return ROYAL;
        }
    }
}
