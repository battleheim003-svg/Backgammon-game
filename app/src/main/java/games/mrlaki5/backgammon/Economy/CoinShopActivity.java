package games.mrlaki5.backgammon.Economy;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.R;

/**
 * Activity for the in-game Coin Shop offering Avatar Frames, Dice Skins, Titles, and Themes.
 */
public class CoinShopActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    private CoinManager coinManager;
    private PlayerProfileManager profileManager;
    private TextView tvCoinBalance;
    private GridView gvItems;
    private ShopAdapter adapter;

    private final List<ShopItem> allItems = new ArrayList<>();
    private final List<ShopItem> displayedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_coin_shop);

        coinManager = new CoinManager(this);
        profileManager = new PlayerProfileManager(this);

        tvCoinBalance = findViewById(R.id.tvShopCoinBalance);
        gvItems = findViewById(R.id.gvShopItems);

        ImageButton btnBack = findViewById(R.id.btnShopBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        buildShopCatalog();
        setupCategoryTabs();
        updateBalance();
        filterItems(null);
    }

    private void updateBalance() {
        if (tvCoinBalance != null && coinManager != null) {
            tvCoinBalance.setText(String.valueOf(coinManager.getBalance()));
        }
    }

    private void buildShopCatalog() {
        allItems.clear();

        // 1. Avatar Frames
        allItems.add(new ShopItem("frame_default", "Classic Frame", "Simple wooden border", 0, ShopItem.Category.AVATAR_FRAME, "🪵"));
        allItems.add(new ShopItem("frame_bronze", "Bronze Frame", "Polished antique bronze border", 200, ShopItem.Category.AVATAR_FRAME, "🥉"));
        allItems.add(new ShopItem("frame_silver", "Silver Frame", "Lustrous sterling silver rim", 500, ShopItem.Category.AVATAR_FRAME, "🥈"));
        allItems.add(new ShopItem("frame_gold", "Gold Frame", "Royal Persian 24k gold leaf", 1000, ShopItem.Category.AVATAR_FRAME, "🥇"));
        allItems.add(new ShopItem("frame_diamond", "Diamond Frame", "Radiant gemstone crown", 2500, ShopItem.Category.AVATAR_FRAME, "💎"));

        // 2. Dice Skins
        allItems.add(new ShopItem("dice_default", "Classic Dice", "Standard white bone dice", 0, ShopItem.Category.DICE_SKIN, "🎲"));
        allItems.add(new ShopItem("dice_ruby", "Ruby Dice", "Crimson jewel dice", 400, ShopItem.Category.DICE_SKIN, "🔴"));
        allItems.add(new ShopItem("dice_emerald", "Emerald Dice", "Vibrant green mineral dice", 750, ShopItem.Category.DICE_SKIN, "🟢"));
        allItems.add(new ShopItem("dice_obsidian", "Obsidian Dice", "Deep volcanic glass dice", 1200, ShopItem.Category.DICE_SKIN, "⚫"));

        // 3. Titles
        allItems.add(new ShopItem("title_beginner", "Beginner", "Starting player title", 0, ShopItem.Category.TITLE, "🌱"));
        allItems.add(new ShopItem("title_strategist", "Strategist", "Clever tactician on the board", 300, ShopItem.Category.TITLE, "🧠"));
        allItems.add(new ShopItem("title_master", "Master", "Experienced backgammon player", 800, ShopItem.Category.TITLE, "👑"));
        allItems.add(new ShopItem("title_grandmaster", "Grandmaster", "Legendary master of Takhteh", 2000, ShopItem.Category.TITLE, "🏆"));

        // 4. Themes
        allItems.add(new ShopItem("theme_pop_art", "Pop Art Board", "Comic-book style board & chips", 350, ShopItem.Category.THEME, "🎨"));
        allItems.add(new ShopItem("theme_cyberpunk", "Cyberpunk Board", "Neon futuristic board & glow", 500, ShopItem.Category.THEME, "⚡"));
        allItems.add(new ShopItem("theme_luxury", "Luxury Persian", "Ivory & gold satin board", 750, ShopItem.Category.THEME, "🏛️"));
    }

    private void setupCategoryTabs() {
        RadioGroup rgCategories = findViewById(R.id.rgShopCategories);
        if (rgCategories != null) {
            rgCategories.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbCategoryFrames) {
                    filterItems(ShopItem.Category.AVATAR_FRAME);
                } else if (checkedId == R.id.rbCategoryDice) {
                    filterItems(ShopItem.Category.DICE_SKIN);
                } else if (checkedId == R.id.rbCategoryTitles) {
                    filterItems(ShopItem.Category.TITLE);
                } else if (checkedId == R.id.rbCategoryThemes) {
                    filterItems(ShopItem.Category.THEME);
                } else {
                    filterItems(null);
                }
            });
        }
    }

    private void filterItems(ShopItem.Category category) {
        displayedItems.clear();
        for (ShopItem item : allItems) {
            if (category == null || item.getCategory() == category) {
                displayedItems.add(item);
            }
        }

        if (adapter == null) {
            adapter = new ShopAdapter(this, displayedItems, profileManager, coinManager, this::updateBalance);
            gvItems.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}
