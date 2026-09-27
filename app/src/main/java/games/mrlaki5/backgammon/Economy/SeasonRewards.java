package games.mrlaki5.backgammon.Economy;

import android.content.Context;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;

/**
 * The pieces that coins cannot buy.
 *
 * Every other object in this game has a price, and a catalogue where everything
 * has a price is one where nothing was earned — the shelf fills up at whatever
 * rate the player is willing to spend, and none of it means anything afterwards.
 * The four season banners are the answer to that: they are handed over here,
 * when a Journey chapter closes, and nowhere else. No screen offers them, the
 * bundle deliberately skips them, and a player who did not play that chapter
 * while its season was open does not get one later.
 *
 * That is the whole mechanism. It is small because it has to be: the moment
 * there is a second way to get a banner, there is no reason to want one.
 */
public final class SeasonRewards {

    private SeasonRewards() {}

    /**
     * The chapter each banner closes behind.
     *
     * Harbour is chapter eight because chapter eight is the harbour; the others
     * are spaced so a banner never lands two chapters running.
     */
    private static String bannerForStage(int stageIndex) {
        switch (stageIndex) {
            case 1: return "banner_isfahan";     // the tea house
            case 4: return "banner_neyshabur";   // Crete
            case 7: return "banner_harbour";     // the harbour
            case 8: return "banner_caravan";     // the grand master
            default: return null;
        }
    }

    /**
     * Hands over the banner a chapter carries, if it carries one and the player
     * does not already hold it.
     *
     * @return the id of the piece granted, or null if this chapter grants none.
     */
    public static String grantForStage(Context context, int stageIndex) {
        String id = bannerForStage(stageIndex);
        if (id == null) {
            return null;
        }
        PlayerProfileManager profile = PlayerProfileManager.getInstance(context);
        if (profile.isItemPurchased(id)) {
            return null;
        }
        profile.addPurchasedItem(id);
        return id;
    }

    /** The chapter that hands over this piece, or -1 if none does. */
    public static int stageForItem(String itemId) {
        for (int i = 0; i < 9; i++) {
            if (itemId.equals(bannerForStage(i))) {
                return i;
            }
        }
        return -1;
    }
}
