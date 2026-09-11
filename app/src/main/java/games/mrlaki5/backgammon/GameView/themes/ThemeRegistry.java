package games.mrlaki5.backgammon.GameView.themes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.mrlaki5.backgammon.GamePreferences;

/**
 * Registry listing all registered backgammon themes, their IDs, and display names.
 */
public final class ThemeRegistry {

    public static class ThemeInfo {
        private final int id;
        private final String key;
        private final String nameEn;
        private final String nameFa;
        private final boolean free;

        public ThemeInfo(int id, String key, String nameEn, String nameFa, boolean free) {
            this.id = id;
            this.key = key;
            this.nameEn = nameEn;
            this.nameFa = nameFa;
            this.free = free;
        }

        public int getId() { return id; }
        public String getKey() { return key; }
        public String getNameEn() { return nameEn; }
        public String getNameFa() { return nameFa; }
        public boolean isFree() { return free; }
    }

    private static final List<ThemeInfo> THEMES;

    static {
        List<ThemeInfo> list = new ArrayList<>();
        list.add(new ThemeInfo(GamePreferences.THEME_ROYAL, "theme_royal", "Royal", "سلطنتی", true));
        list.add(new ThemeInfo(GamePreferences.THEME_POP_ART, "theme_pop_art", "Pop Art", "پاپ آرت", true));
        list.add(new ThemeInfo(GamePreferences.THEME_CYBERPUNK, "theme_cyberpunk", "Cyberpunk", "سایبرپانک", true));
        list.add(new ThemeInfo(GamePreferences.THEME_LUXURY, "theme_luxury", "Luxury", "لاکچری", false));
        list.add(new ThemeInfo(GamePreferences.THEME_WOODLAND, "theme_woodland", "Woodland", "جنگلی", true));
        list.add(new ThemeInfo(GamePreferences.THEME_GALAXY, "theme_galaxy", "Galaxy", "کهکشان", false));
        list.add(new ThemeInfo(GamePreferences.THEME_ANCIENT_EGYPT, "theme_ancient_egypt", "Ancient Egypt", "مصر باستان", false));
        list.add(new ThemeInfo(GamePreferences.THEME_NEON_RETRO, "theme_neon_retro", "Neon Retro", "نئون رترو", false));
        THEMES = Collections.unmodifiableList(list);
    }

    private ThemeRegistry() {}

    public static List<ThemeInfo> getAllThemes() {
        return THEMES;
    }

    public static ThemeInfo getThemeInfo(int id) {
        for (ThemeInfo info : THEMES) {
            if (info.getId() == id) {
                return info;
            }
        }
        return THEMES.get(0);
    }

    public static String getThemeName(int id) {
        return getThemeInfo(id).getNameEn();
    }
}