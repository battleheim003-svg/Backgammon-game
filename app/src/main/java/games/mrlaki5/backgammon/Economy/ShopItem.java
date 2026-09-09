package games.mrlaki5.backgammon.Economy;

/**
 * Model class for an item available in the Coin Shop.
 */
public class ShopItem {

    public enum Category {
        AVATAR_FRAME,
        DICE_SKIN,
        TITLE,
        THEME
    }

    private final String id;
    private final String title;
    private final String description;
    private final int price;
    private final Category category;
    private final String iconEmoji;

    public ShopItem(String id, String title, String description, int price, Category category, String iconEmoji) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.iconEmoji = iconEmoji;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public boolean isFree() {
        return price <= 0;
    }
}
