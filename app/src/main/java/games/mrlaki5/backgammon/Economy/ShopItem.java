package games.mrlaki5.backgammon.Economy;

import android.content.Context;

import games.mrlaki5.backgammon.R;

public class ShopItem {

    public enum Category {
        ALL,
        AVATAR_FRAME,
        DICE_SKIN,
        TITLE,
        THEME,
        COSMETIC,
        CONSUMABLE,
        BUNDLE
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY;

        public String hexColor() {
            switch (this) {
                case COMMON:    return "#9E9E9E";
                case RARE:      return "#2196F3";
                case EPIC:      return "#9C27B0";
                case LEGENDARY: return "#FFB300";
                default:        return "#9E9E9E";
            }
        }

        /** Locale-aware rarity label — pass a Context (e.g. from the RecyclerView item's view). */
        public String label(Context context) {
            switch (this) {
                case COMMON:    return context.getString(R.string.shop_rarity_common);
                case RARE:      return context.getString(R.string.shop_rarity_rare);
                case EPIC:      return context.getString(R.string.shop_rarity_epic);
                case LEGENDARY: return context.getString(R.string.shop_rarity_legendary);
                default:        return "";
            }
        }
    }

    public final String id;
    public final String title;
    public final String description;
    public final int price;
    public final Category category;
    public final Rarity rarity;
    public final String iconEmoji;
    public final int iconRes;
    public final int nameRes;
    public final int unlockRequirement;
    public final String badge;
    public final int quantity;
    public final boolean isConsumable;

    public ShopItem(String id, String title, String description, int price,
                    Category category, Rarity rarity, String iconEmoji,
                    int iconRes, int nameRes, int unlockRequirement,
                    String badge, int quantity, boolean isConsumable) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.rarity = rarity;
        this.iconEmoji = iconEmoji;
        this.iconRes = iconRes;
        this.nameRes = nameRes;
        this.unlockRequirement = unlockRequirement;
        this.badge = badge;
        this.quantity = quantity;
        this.isConsumable = isConsumable;
    }

    public ShopItem(String id, String title, String description, int price,
                    Category category, Rarity rarity, String iconEmoji,
                    int unlockRequirement, String badge) {
        this(id, title, description, price, category, rarity, iconEmoji,
                0, 0, unlockRequirement, badge, 0, category == Category.CONSUMABLE);
    }

    public static ShopItem permanent(String id, String title, String description, int price,
                                     Category category, Rarity rarity, String iconEmoji) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, 0, null);
    }

    public static ShopItem withUnlock(String id, String title, String description, int price,
                                      Category category, Rarity rarity, String iconEmoji,
                                      int minWins) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, minWins, null);
    }

    public static ShopItem badged(String id, String title, String description, int price,
                                   Category category, Rarity rarity, String iconEmoji, String badge) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, 0, badge);
    }

    // Consumable factory methods
    public static ShopItem hintPack3(Context context) {
        return new ShopItem("hint_pack_3", context.getString(R.string.shop_hint_pack_3),
                context.getString(R.string.shop_hint_pack_3_desc), 50,
                Category.CONSUMABLE, Rarity.COMMON, "💡",
                R.drawable.ic_shop_hint, R.string.shop_hint_pack_3,
                0, null, 3, true);
    }

    public static ShopItem hintPack10(Context context) {
        return new ShopItem("hint_pack_10", context.getString(R.string.shop_hint_pack_10),
                context.getString(R.string.shop_hint_pack_10_desc), 150,
                Category.CONSUMABLE, Rarity.RARE, "💡",
                R.drawable.ic_shop_hint, R.string.shop_hint_pack_10,
                0, context.getString(R.string.shop_badge_discount), 10, true);
    }

    public static ShopItem undoPack3(Context context) {
        return new ShopItem("undo_pack_3", context.getString(R.string.shop_undo_pack_3),
                context.getString(R.string.shop_undo_pack_3_desc), 65,
                Category.CONSUMABLE, Rarity.COMMON, "↩️",
                R.drawable.ic_shop_undo, R.string.shop_undo_pack_3,
                0, null, 3, true);
    }

    public static ShopItem starterBundle(Context context) {
        return new ShopItem("starter_bundle", context.getString(R.string.shop_starter_bundle),
                context.getString(R.string.shop_starter_bundle_desc), 340,
                Category.BUNDLE, Rarity.EPIC, "🎁",
                R.drawable.ic_shop_bundle, R.string.shop_starter_bundle,
                0, context.getString(R.string.shop_badge_special), 0, false);
    }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public int getPrice()              { return price; }
    public Category getCategory()      { return category; }
    public Rarity getRarity()          { return rarity; }
    public String getIconEmoji()       { return iconEmoji; }
    public int getIconRes()            { return iconRes; }
    public int getNameRes()            { return nameRes; }
    public int getUnlockRequirement()  { return unlockRequirement; }
    public String getBadge()           { return badge; }
    public int getQuantity()           { return quantity; }
    public boolean isConsumable()      { return isConsumable; }
    public boolean isFree()            { return price <= 0; }
    public boolean hasUnlockReq()      { return unlockRequirement > 0; }
}
