package games.mrlaki5.backgammon.Economy;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import games.mrlaki5.backgammon.R;

/**
 * The crafted goods: checkers, dice and titles.
 *
 * These live here rather than inside the shop screen because two screens need
 * the same list — the shop, to sell them, and the treasury, to show which ones
 * were missed. A season names its pieces by id, so both screens have to agree on
 * what those ids mean, and the only way to guarantee that is one definition.
 */
public final class SeasonCatalogue {

    private SeasonCatalogue() {}

    public static List<ShopItem> all(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.addAll(boards(context));
        items.addAll(checkers(context));
        items.addAll(dice(context));
        items.addAll(effects(context));
        items.addAll(banners(context));
        items.addAll(frames(context));
        items.addAll(titles(context));
        return items;
    }

    /** Every piece in one season's set, in the order the bundle page lists them. */
    public static List<ShopItem> ofSeason(Context context, Season season) {
        List<ShopItem> set = new ArrayList<>();
        for (String id : season.itemIds()) {
            ShopItem item = byId(context, id);
            if (item != null) {
                set.add(item);
            }
        }
        return set;
    }

    public static ShopItem byId(Context context, String id) {
        for (ShopItem item : all(context)) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    /**
     * What the pieces of a set would cost bought one at a time.
     *
     * Earn-only pieces contribute nothing, because they are not for sale at any
     * price and counting them would inflate the saving the bundle claims.
     */
    public static int separatePrice(Context context, Season season) {
        int total = 0;
        for (ShopItem item : ofSeason(context, season)) {
            if (item.isPurchasable()) {
                total += item.price;
            }
        }
        return total;
    }

    /**
     * The playing surface. A board is the largest thing on screen during a
     * game, so it is the piece a set is actually judged on — and the catalogue
     * had none at all.
     */
    public static List<ShopItem> boards(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(board(context, "board_khatam", R.string.board_khatam_title,
                R.string.board_khatam_desc, R.string.board_khatam_story,
                3200, ShopItem.Rarity.EPIC, R.drawable.board_khatam));
        items.add(board(context, "board_mina", R.string.board_mina_title,
                R.string.board_mina_desc, R.string.board_mina_story,
                3600, ShopItem.Rarity.EPIC, R.drawable.board_mina));
        items.add(board(context, "board_nacre", R.string.board_nacre_title,
                R.string.board_nacre_desc, R.string.board_nacre_story,
                3000, ShopItem.Rarity.EPIC, R.drawable.board_nacre));
        items.add(board(context, "board_monabbat", R.string.board_monabbat_title,
                R.string.board_monabbat_desc, R.string.board_monabbat_story,
                3800, ShopItem.Rarity.EPIC, R.drawable.board_monabbat));
        return items;
    }

    private static ShopItem board(Context c, String id, int title, int desc, int story,
                                  int price, ShopItem.Rarity rarity, int art) {
        return ShopItem.crafted(id, c.getString(title), c.getString(desc),
                c.getString(story), price, ShopItem.Category.BOARD, rarity, art,
                0, ShopItem.Acquisition.BUY, "");
    }

    /** What the dice throw off when they land. */
    public static List<ShopItem> effects(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(effect(context, "effect_enamel_dust", R.string.effect_enamel_dust_title,
                R.string.effect_enamel_dust_desc, R.string.effect_enamel_dust_story,
                1400, R.drawable.effect_enamel_dust));
        items.add(effect(context, "effect_turquoise_spark", R.string.effect_turquoise_spark_title,
                R.string.effect_turquoise_spark_desc, R.string.effect_turquoise_spark_story,
                1600, R.drawable.effect_turquoise_spark));
        items.add(effect(context, "effect_pearl_ripple", R.string.effect_pearl_ripple_title,
                R.string.effect_pearl_ripple_desc, R.string.effect_pearl_ripple_story,
                1300, R.drawable.effect_pearl_ripple));
        items.add(effect(context, "effect_agate_ember", R.string.effect_agate_ember_title,
                R.string.effect_agate_ember_desc, R.string.effect_agate_ember_story,
                1700, R.drawable.effect_agate_ember));
        return items;
    }

    private static ShopItem effect(Context c, String id, int title, int desc, int story,
                                   int price, int art) {
        return ShopItem.crafted(id, c.getString(title), c.getString(desc),
                c.getString(story), price, ShopItem.Category.DICE_EFFECT,
                ShopItem.Rarity.RARE, art, 0, ShopItem.Acquisition.BUY, "");
    }

    /**
     * Banners. One per season, and none of them has a price.
     *
     * This is the half of the catalogue that gives the other half its meaning:
     * a shelf where everything can be bought is a shelf where nothing was
     * earned, so the banner of each season is only ever handed over for
     * finishing that season's Journey chapter.
     */
    public static List<ShopItem> banners(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(banner(context, "banner_isfahan", R.string.banner_isfahan_title,
                R.string.banner_isfahan_desc, R.string.banner_isfahan_story,
                R.drawable.banner_isfahan, EARN_CHAPTER_ISFAHAN));
        items.add(banner(context, "banner_neyshabur", R.string.banner_neyshabur_title,
                R.string.banner_neyshabur_desc, R.string.banner_neyshabur_story,
                R.drawable.banner_neyshabur, EARN_CHAPTER_NEYSHABUR));
        items.add(banner(context, "banner_harbour", R.string.banner_harbour_title,
                R.string.banner_harbour_desc, R.string.banner_harbour_story,
                R.drawable.banner_harbour, EARN_CHAPTER_HARBOUR));
        items.add(banner(context, "banner_caravan", R.string.banner_caravan_title,
                R.string.banner_caravan_desc, R.string.banner_caravan_story,
                R.drawable.banner_caravan, EARN_CHAPTER_CARAVAN));
        return items;
    }

    /** The Journey chapters that hand over each season's banner. */
    public static final String EARN_CHAPTER_ISFAHAN   = "chapter_isfahan";
    public static final String EARN_CHAPTER_NEYSHABUR = "chapter_neyshabur";
    public static final String EARN_CHAPTER_HARBOUR   = "chapter_harbour";
    public static final String EARN_CHAPTER_CARAVAN   = "chapter_caravan";

    private static ShopItem banner(Context c, String id, int title, int desc, int story,
                                   int art, String earnKey) {
        return ShopItem.earned(id, c.getString(title), c.getString(desc),
                c.getString(story), ShopItem.Category.BANNER,
                ShopItem.Rarity.EPIC, art, earnKey);
    }

    /** Frames for the player's face, named for a season rather than a metal tier. */
    public static List<ShopItem> frames(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(frame(context, "frame_isfahan", R.string.frame_isfahan_title,
                R.string.frame_isfahan_desc, R.string.frame_isfahan_story,
                1200, R.drawable.frame_isfahan));
        items.add(frame(context, "frame_neyshabur", R.string.frame_neyshabur_title,
                R.string.frame_neyshabur_desc, R.string.frame_neyshabur_story,
                1400, R.drawable.frame_neyshabur));
        items.add(frame(context, "frame_harbour", R.string.frame_harbour_title,
                R.string.frame_harbour_desc, R.string.frame_harbour_story,
                1100, R.drawable.frame_harbour));
        items.add(frame(context, "frame_caravan", R.string.frame_caravan_title,
                R.string.frame_caravan_desc, R.string.frame_caravan_story,
                1500, R.drawable.frame_caravan));
        return items;
    }

    private static ShopItem frame(Context c, String id, int title, int desc, int story,
                                  int price, int art) {
        return ShopItem.crafted(id, c.getString(title), c.getString(desc),
                c.getString(story), price, ShopItem.Category.AVATAR_FRAME,
                ShopItem.Rarity.RARE, art, 0, ShopItem.Acquisition.BUY, "");
    }

    /**
     * Checker sets, named for what a real set is made of.
     *
     * A bronze/silver/gold ladder belongs to every mobile game; walnut, camel
     * bone, Isfahan khatam and Neyshabur turquoise belong to this one.
     */
    public static List<ShopItem> checkers(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(ShopItem.crafted("checkers_walnut",
                context.getString(R.string.checkers_walnut_title),
                context.getString(R.string.checkers_walnut_desc), 0,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.COMMON,
                R.drawable.checkers_walnut, 0));
        items.add(ShopItem.crafted("checkers_bone",
                context.getString(R.string.checkers_bone_title),
                context.getString(R.string.checkers_bone_desc), 300,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.COMMON,
                R.drawable.checkers_bone, 0));
        items.add(ShopItem.crafted("checkers_khatam",
                context.getString(R.string.checkers_khatam_title),
                context.getString(R.string.checkers_khatam_desc), 800,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.RARE,
                R.drawable.checkers_khatam, 15));
        items.add(ShopItem.crafted("checkers_nacre",
                context.getString(R.string.checkers_nacre_title),
                context.getString(R.string.checkers_nacre_desc), 1100,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.RARE,
                R.drawable.checkers_nacre, 25));
        items.add(ShopItem.crafted("checkers_turquoise",
                context.getString(R.string.checkers_turquoise_title),
                context.getString(R.string.checkers_turquoise_desc), 2000,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.EPIC,
                R.drawable.checkers_turquoise, 50));
        items.add(ShopItem.crafted("checkers_agate",
                context.getString(R.string.checkers_agate_title),
                context.getString(R.string.checkers_agate_desc), 2800,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.EPIC,
                R.drawable.checkers_agate, 80));
        items.add(ShopItem.crafted("checkers_gold",
                context.getString(R.string.checkers_gold_title),
                context.getString(R.string.checkers_gold_desc), 6000,
                ShopItem.Category.CHECKERS, ShopItem.Rarity.LEGENDARY,
                R.drawable.checkers_gold, 200));
        return items;
    }

    /** Dice, cut from the stones a real pair is cut from. */
    public static List<ShopItem> dice(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(ShopItem.crafted("dice_bone",
                context.getString(R.string.dice_bone_title),
                context.getString(R.string.dice_bone_desc), 0,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON,
                R.drawable.dice_set_bone, 0));
        items.add(ShopItem.crafted("dice_walnut",
                context.getString(R.string.dice_walnut_title),
                context.getString(R.string.dice_walnut_desc), 250,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON,
                R.drawable.dice_set_walnut, 0));
        items.add(ShopItem.crafted("dice_ebony",
                context.getString(R.string.dice_ebony_title),
                context.getString(R.string.dice_ebony_desc), 700,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE,
                R.drawable.dice_set_ebony, 12));
        items.add(ShopItem.crafted("dice_turquoise",
                context.getString(R.string.dice_turquoise_title),
                context.getString(R.string.dice_turquoise_desc), 1800,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC,
                R.drawable.dice_set_turquoise, 40));
        items.add(ShopItem.crafted("dice_agate",
                context.getString(R.string.dice_agate_title),
                context.getString(R.string.dice_agate_desc), 2400,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC,
                R.drawable.dice_set_agate, 60));
        items.add(ShopItem.crafted("dice_gold",
                context.getString(R.string.dice_gold_title),
                context.getString(R.string.dice_gold_desc), 5000,
                ShopItem.Category.DICE_SKIN, ShopItem.Rarity.LEGENDARY,
                R.drawable.dice_set_gold, 150));
        return items;
    }

    /**
     * Titles in the game's own language rather than a rank ladder — what
     * backgammon players actually call each other.
     */
    public static List<ShopItem> titles(Context context) {
        List<ShopItem> items = new ArrayList<>();
        items.add(ShopItem.permanent("title_none",
                context.getString(R.string.title_none_title),
                context.getString(R.string.title_none_desc), 0,
                ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, ""));
        items.add(ShopItem.withUnlock("title_doubler",
                context.getString(R.string.title_doubler_title),
                context.getString(R.string.title_doubler_desc), 400,
                ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "", 10));
        items.add(ShopItem.withUnlock("title_prime",
                context.getString(R.string.title_prime_title),
                context.getString(R.string.title_prime_desc), 900,
                ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "", 25));
        items.add(ShopItem.withUnlock("title_gammon",
                context.getString(R.string.title_gammon_title),
                context.getString(R.string.title_gammon_desc), 1500,
                ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "", 40));
        items.add(ShopItem.withUnlock("title_plakoto",
                context.getString(R.string.title_plakoto_title),
                context.getString(R.string.title_plakoto_desc), 2200,
                ShopItem.Category.TITLE, ShopItem.Rarity.EPIC, "", 70));
        items.add(ShopItem.withUnlock("title_unbeaten",
                context.getString(R.string.title_unbeaten_title),
                context.getString(R.string.title_unbeaten_desc), 4000,
                ShopItem.Category.TITLE, ShopItem.Rarity.LEGENDARY, "", 150));
        return items;
    }
}
