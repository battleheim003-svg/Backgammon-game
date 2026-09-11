package games.mrlaki5.backgammon.Economy;

import games.mrlaki5.backgammon.R;

public class ShopItem {

    public enum Category {
        ALL,
        AVATAR_FRAME,
        DICE_SKIN,
        TITLE,
        THEME,
        RENTAL,
        COSMETIC,
        CONSUMABLE,
        BUNDLE
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY;

        /** Color int for this rarity (use Color.parseColor in views). */
        public String hexColor() {
            switch (this) {
                case COMMON:    return "#9E9E9E";
                case RARE:      return "#2196F3";
                case EPIC:      return "#9C27B0";
                case LEGENDARY: return "#FFB300";
                default:        return "#9E9E9E";
            }
        }

        public String label() {
            switch (this) {
                case COMMON:    return "معمولی";
                case RARE:      return "نادر";
                case EPIC:      return "حماسی";
                case LEGENDARY: return "افسانه‌ای";
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
    public final int unlockRequirement; // 0 = no requirement; else min total wins
    public final boolean isRental;      // true = 24h rental, not permanent
    public final String badge;          // null | "NEW" | "HOT" | "OFFER"
    public final int quantity;          // default 0 for non-consumables
    public final boolean isConsumable;

    public ShopItem(String id, String title, String description, int price,
                    Category category, Rarity rarity, String iconEmoji,
                    int iconRes, int nameRes, int unlockRequirement,
                    boolean isRental, String badge, int quantity, boolean isConsumable) {
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
        this.isRental = isRental;
        this.badge = badge;
        this.quantity = quantity;
        this.isConsumable = isConsumable;
    }

    public ShopItem(String id, String title, String description, int price,
                    Category category, Rarity rarity, String iconEmoji,
                    int unlockRequirement, boolean isRental, String badge) {
        this(id, title, description, price, category, rarity, iconEmoji,
                0, 0, unlockRequirement, isRental, badge, 0, category == Category.CONSUMABLE);
    }

    // Convenience constructors
    public static ShopItem permanent(String id, String title, String description, int price,
                                     Category category, Rarity rarity, String iconEmoji) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, 0, false, null);
    }

    public static ShopItem withUnlock(String id, String title, String description, int price,
                                      Category category, Rarity rarity, String iconEmoji,
                                      int minWins) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, minWins, false, null);
    }

    public static ShopItem rental(String id, String title, String description, int price,
                                  Category originalCategory, Rarity rarity, String iconEmoji) {
        return new ShopItem(id, title, description, price, Category.RENTAL, rarity, iconEmoji, 0, true, "اجاره");
    }

    public static ShopItem badged(String id, String title, String description, int price,
                                   Category category, Rarity rarity, String iconEmoji, String badge) {
        return new ShopItem(id, title, description, price, category, rarity, iconEmoji, 0, false, badge);
    }

    // Consumable items
    public static ShopItem hintPack3() {
        return new ShopItem("hint_pack_3", "۳ راهنما", "۳ شارژ راهنما برای بازی", 50,
                Category.CONSUMABLE, Rarity.COMMON, "💡",
                R.drawable.ic_shop_hint, R.string.shop_hint_pack_3,
                0, false, null, 3, true);
    }

    public static ShopItem hintPack10() {
        return new ShopItem("hint_pack_10", "۱۰ راهنما", "۱۰ شارژ راهنما برای بازی", 150,
                Category.CONSUMABLE, Rarity.RARE, "💡",
                R.drawable.ic_shop_hint, R.string.shop_hint_pack_10,
                0, false, "تخفیف", 10, true);
    }

    public static ShopItem undoPack3() {
        return new ShopItem("undo_pack_3", "۳ برگشت", "۳ شارژ برگشت حرکت رایگان", 65,
                Category.CONSUMABLE, Rarity.COMMON, "↩️",
                R.drawable.ic_shop_undo, R.string.shop_undo_pack_3,
                0, false, null, 3, true);
    }

    public static ShopItem starterBundle() {
        return new ShopItem("starter_bundle", "باندل شروع", "فریم نقره + تاس استیل + ۵۰ سکه هدیه + ۲ راهنما", 340,
                Category.BUNDLE, Rarity.EPIC, "🎁",
                R.drawable.ic_shop_bundle, R.string.shop_starter_bundle,
                0, false, "ویژه", 0, false);
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
    public boolean isRental()          { return isRental; }
    public String getBadge()           { return badge; }
    public int getQuantity()           { return quantity; }
    public boolean isConsumable()      { return isConsumable; }
    public boolean isFree()            { return price <= 0; }
    public boolean hasUnlockReq()      { return unlockRequirement > 0; }
}