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
            new String[] {"checkers_khatam", "dice_ebony", "title_prime"},
            1900),

    /** Neyshabur: the mines the country's blue is named after. */
    NEYSHABUR(2, R.string.season_neyshabur_name, R.string.season_neyshabur_story,
            R.string.season_neyshabur_bundle,
            new String[] {"checkers_turquoise", "dice_turquoise", "title_gammon"},
            3400),

    /** The Gulf: shell, coral, and everything the divers brought up. */
    HARBOUR(3, R.string.season_harbour_name, R.string.season_harbour_story,
            R.string.season_harbour_bundle,
            new String[] {"checkers_nacre", "dice_bone", "title_doubler"},
            1600),

    /** Yemen by way of the caravan road: banded agate, cut like a seal stone. */
    CARAVAN(4, R.string.season_caravan_name, R.string.season_caravan_story,
            R.string.season_caravan_bundle,
            new String[] {"checkers_agate", "dice_agate", "title_plakoto"},
            4200);

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
