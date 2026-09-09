package games.mrlaki5.backgammon.Economy;

public class ShopItem {

    public enum Category {
        AVATAR_FRAME,
        DICE_SKIN,
        TITLE,
        THEME,
        RENTAL
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

    private final String id;
    private final String title;
    private final String description;
    private final int price;
    private final Category category;
    private final Rarity rarity;
    private final String iconEmoji;
    private final int unlockRequirement; // 0 = no requirement; else min total wins
    private final boolean isRental;      // true = 24h rental, not permanent
    private final String badge;          // null | "NEW" | "HOT" | "OFFER"

    public ShopItem(String id, String title, String description, int price,
                    Category category, Rarity rarity, String iconEmoji,
                    int unlockRequirement, boolean isRental, String badge) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.rarity = rarity;
        this.iconEmoji = iconEmoji;
        this.unlockRequirement = unlockRequirement;
        this.isRental = isRental;
        this.badge = badge;
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

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public int getPrice()              { return price; }
    public Category getCategory()      { return category; }
    public Rarity getRarity()          { return rarity; }
    public String getIconEmoji()       { return iconEmoji; }
    public int getUnlockRequirement()  { return unlockRequirement; }
    public boolean isRental()          { return isRental; }
    public String getBadge()           { return badge; }
    public boolean isFree()            { return price <= 0; }
    public boolean hasUnlockReq()      { return unlockRequirement > 0; }
}
