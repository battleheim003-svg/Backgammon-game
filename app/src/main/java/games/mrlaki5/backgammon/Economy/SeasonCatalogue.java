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
        items.addAll(checkers(context));
        items.addAll(dice(context));
        items.addAll(titles(context));
        return items;
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
