package games.mrlaki5.backgammon.Economy;

import games.mrlaki5.backgammon.R;

/**
 * A season: one workshop, one craft, one matched set — offered for six weeks and
 * then never again.
 *
 * What makes a collection worth having is not how well its pieces are drawn but
 * that some of them cannot be got any more. A shelf that always has everything
 * on it is a catalogue; a shelf that closes is a collection. So each season is
 * built around a single Persian craft city, ships a set whose checkers, dice,
 * board and title all come from that one workshop, and retires when the season
 * ends: still owned by whoever bought it, no longer sold to anyone else.
 *
 * The clock is the device's own calendar, because this game is offline and has
 * no server to ask. Six-week windows counted from a fixed epoch give every
 * player the same season on the same day without a single request.
 */
public enum Season {

    /** Isfahan: khatam marquetry, enamel, chased metal. */
    ISFAHAN(1, R.string.season_isfahan_name, R.string.season_isfahan_story,
            R.string.season_isfahan_bundle,
            new String[] {"board_khatam", "checkers_khatam", "dice_ebony",
                          "effect_enamel_dust", "banner_isfahan", "frame_isfahan",
                          "title_prime"},
            5900),

    /** Neyshabur: the mines the country's blue is named after. */
    NEYSHABUR(2, R.string.season_neyshabur_name, R.string.season_neyshabur_story,
            R.string.season_neyshabur_bundle,
            new String[] {"board_mina", "checkers_turquoise", "dice_turquoise",
                          "effect_turquoise_spark", "banner_neyshabur",
                          "frame_neyshabur", "title_gammon"},
            8500),

    /** The Gulf: shell, coral, and everything the divers brought up. */
    HARBOUR(3, R.string.season_harbour_name, R.string.season_harbour_story,
            R.string.season_harbour_bundle,
            new String[] {"board_nacre", "checkers_nacre", "dice_bone",
                          "effect_pearl_ripple", "banner_harbour", "frame_harbour",
                          "title_doubler"},
            4900),

    /** Yemen by way of the caravan road: banded agate, cut like a seal stone. */
    CARAVAN(4, R.string.season_caravan_name, R.string.season_caravan_story,
            R.string.season_caravan_bundle,
            new String[] {"board_monabbat", "checkers_agate", "dice_agate",
                          "effect_agate_ember", "banner_caravan", "frame_caravan",
                          "title_plakoto"},
            10300);

    /** Six weeks, in days — long enough to earn a set, short enough to matter. */
    public static final int LENGTH_DAYS = 42;

    public final int number;
    public final int nameRes;
    public final int storyRes;
    public final int bundleNameRes;
    private final String[] itemIds;
    public final int bundlePrice;

    Season(int number, int nameRes, int storyRes, int bundleNameRes,
           String[] itemIds, int bundlePrice) {
        this.number = number;
        this.nameRes = nameRes;
        this.storyRes = storyRes;
        this.bundleNameRes = bundleNameRes;
        this.itemIds = itemIds;
        this.bundlePrice = bundlePrice;
    }

    /** The products this season's set is made of. */
    public String[] itemIds() {
        return itemIds.clone();
    }

    public boolean contains(String itemId) {
        for (String id : itemIds) {
            if (id.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The image the bundle page leads with: this season's whole set standing in
     * its niche. A set sold as a list of names is a receipt; this is the thing
     * a player actually decides on.
     */
    public int heroDrawable() {
        switch (this) {
            case NEYSHABUR: return R.drawable.hero_season_2;
            case HARBOUR:   return R.drawable.hero_season_3;
            case CARAVAN:   return R.drawable.hero_season_4;
            case ISFAHAN:
            default:        return R.drawable.hero_season_1;
        }
    }

    /** The Journey chapter whose completion hands over this season's banner. */
    public String earnChapterKey() {
        switch (this) {
            case NEYSHABUR: return SeasonCatalogue.EARN_CHAPTER_NEYSHABUR;
            case HARBOUR:   return SeasonCatalogue.EARN_CHAPTER_HARBOUR;
            case CARAVAN:   return SeasonCatalogue.EARN_CHAPTER_CARAVAN;
            case ISFAHAN:
            default:        return SeasonCatalogue.EARN_CHAPTER_ISFAHAN;
        }
    }

    /** The season a piece belongs to, or null if it is not seasonal at all. */
    public static Season of(String itemId) {
        for (Season season : values()) {
            if (season.contains(itemId)) {
                return season;
            }
        }
        return null;
    }

    /** The seal stamped on anything from this season, in its own metal. */
    public int sealDrawable() {
        switch (this) {
            case NEYSHABUR: return R.drawable.seal_season_2;
            case HARBOUR:   return R.drawable.seal_season_3;
            case CARAVAN:   return R.drawable.seal_season_4;
            case ISFAHAN:
            default:        return R.drawable.seal_season_1;
        }
    }
}
