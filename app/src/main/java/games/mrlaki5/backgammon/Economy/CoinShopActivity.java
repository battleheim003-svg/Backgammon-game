package games.mrlaki5.backgammon.Economy;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Monetization.ads.AdCallback;
import games.mrlaki5.backgammon.Monetization.ads.AdManager;
import games.mrlaki5.backgammon.Monetization.ads.RewardedAdPlacement;
import games.mrlaki5.backgammon.Monetization.ads.RewardedAdTracker;
import games.mrlaki5.backgammon.Monetization.ads.RewardedAdUiHelper;
import games.mrlaki5.backgammon.R;

/**
 * Activity for the in-game Coin Shop offering Avatar Frames, Dice Skins, Titles, Themes,
 * Consumables, and Bundles using a 2-column RecyclerView with category tabs.
 */
public class CoinShopActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    private CoinManager coinManager;
    private PlayerProfileManager profileManager;
    private RewardedAdTracker rewardedAdTracker;
    private TextView tvCoinBalance;
    private RecyclerView rvShopItems;
    private ShopAdapter shopAdapter;
    private ShopItem.Category selectedCategory = ShopItem.Category.ALL;
    private Button btnFreeCoinsShop;

    private final List<ShopItem> allItems = new ArrayList<>();

    private final android.os.Handler freeCoinsTickHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable freeCoinsTick = new Runnable() {
        @Override
        public void run() {
            refreshFreeCoinsButton();
            freeCoinsTickHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_coin_shop);

        coinManager = new CoinManager(this);
        profileManager = PlayerProfileManager.getInstance(this);
        rewardedAdTracker = new RewardedAdTracker(this);

        tvCoinBalance = findViewById(R.id.tvShopCoinBalance);
        rvShopItems = findViewById(R.id.shopRecyclerView);

        ImageButton btnBack = findViewById(R.id.btnShopBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        buildShopCatalog();
        setupRecyclerView();
        setupCategoryTabs();
        setupFreeCoinsButton();
        refreshCoinBalance();
    }

    private void setupFreeCoinsButton() {
        btnFreeCoinsShop = findViewById(R.id.btnFreeCoinsShop);
        if (btnFreeCoinsShop == null) return;

        refreshFreeCoinsButton();

        btnFreeCoinsShop.setOnClickListener(v -> {
            if (rewardedAdTracker.isDailyLimitReached(RewardedAdPlacement.FREE_COINS)) {
                Toast.makeText(this, R.string.ad_daily_limit_reached, Toast.LENGTH_SHORT).show();
                return;
            }
            if (rewardedAdTracker.getCooldownRemainingSeconds(RewardedAdPlacement.FREE_COINS) > 0) {
                refreshFreeCoinsButton();
                return;
            }

            AdManager adManager = MenuActivity.getSharedAdManager();
            if (adManager == null || !adManager.isRewardedAdReady()) {
                Toast.makeText(this, R.string.ad_not_ready, Toast.LENGTH_SHORT).show();
                if (adManager != null) adManager.preloadAds();
                return;
            }

            adManager.showRewardedAd(this, RewardedAdPlacement.FREE_COINS, new AdCallback() {
                @Override public void onAdLoaded() {}
                @Override public void onAdFailedToLoad(String error) {
                    runOnUiThread(() -> Toast.makeText(CoinShopActivity.this,
                            R.string.ad_not_ready, Toast.LENGTH_SHORT).show());
                }
                @Override public void onAdShown() {}
                @Override public void onAdDismissed() {}
                @Override public void onAdClicked() {}
                @Override public void onRewardEarned() {
                    runOnUiThread(() -> {
                        coinManager.earn(CoinConfig.REWARDED_AD_WATCH, "free_coins_shop");
                        rewardedAdTracker.recordShow(RewardedAdPlacement.FREE_COINS);
                        refreshCoinBalance();
                        refreshFreeCoinsButton();
                        RewardedAdUiHelper.showRewardDialog(CoinShopActivity.this,
                                CoinConfig.REWARDED_AD_WATCH, () -> {
                                    refreshCoinBalance();
                                    refreshFreeCoinsButton();
                                });
                    });
                }
            });
        });
    }

    private void refreshFreeCoinsButton() {
        if (btnFreeCoinsShop == null || rewardedAdTracker == null) return;
        RewardedAdUiHelper.refreshButton(this, btnFreeCoinsShop, rewardedAdTracker,
                RewardedAdPlacement.FREE_COINS, CoinConfig.REWARDED_AD_WATCH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        freeCoinsTickHandler.removeCallbacks(freeCoinsTick);
        freeCoinsTickHandler.post(freeCoinsTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        freeCoinsTickHandler.removeCallbacks(freeCoinsTick);
    }

    private void refreshCoinBalance() {
        if (tvCoinBalance != null && profileManager != null) {
            tvCoinBalance.setText(String.valueOf(profileManager.getBalance()));
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (shopAdapter != null) {
                    ShopItem item = shopAdapter.getItem(position);
                    if (item != null && item.getCategory() == ShopItem.Category.BUNDLE) {
                        return 2;
                    }
                }
                return 1;
            }
        });
        rvShopItems.setLayoutManager(glm);

        shopAdapter = new ShopAdapter(this, allItems, profileManager, coinManager, this::refreshCoinBalance);
        rvShopItems.setAdapter(shopAdapter);
    }

    private void setupCategoryTabs() {
        LinearLayout tabs = findViewById(R.id.shopCategoryTabs);
        if (tabs == null) return;
        tabs.removeAllViews();

        ShopItem.Category[] cats = {
                ShopItem.Category.ALL,
                ShopItem.Category.COSMETIC,
                ShopItem.Category.CONSUMABLE,
                ShopItem.Category.BUNDLE
        };
        int[] labelRes = {
                R.string.shop_category_all,
                R.string.shop_category_cosmetic,
                R.string.shop_category_consumable,
                R.string.shop_category_bundle
        };
        String[] icons = {"🛍️", "💎", "🎯", "🎁"};

        for (int i = 0; i < cats.length; i++) {
            final ShopItem.Category cat = cats[i];
            Button btn = new Button(new ContextThemeWrapper(this, R.style.ShopCategoryTabButton), null, 0);
            btn.setText(icons[i] + " " + getString(labelRes[i]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMarginEnd(4);
            btn.setLayoutParams(lp);
            btn.setPadding(dp(18), 0, dp(18), 0);
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setSelected(i == 0);
            applyTabAppearance(btn, i == 0);

            btn.setOnClickListener(v -> {
                selectedCategory = cat;
                if (shopAdapter != null) {
                    shopAdapter.filter(cat);
                }
                updateTabSelection(tabs, btn);
            });
            tabs.addView(btn);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void applyTabAppearance(Button btn, boolean selected) {
        btn.setBackgroundResource(selected ? R.drawable.bg_shop_tab_selected : R.drawable.bg_shop_tab_unselected);
        btn.setTextColor(selected ? 0xFF1A1400 : 0xFFCFE3EE);
    }

    private void updateTabSelection(LinearLayout tabs, Button selected) {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            View v = tabs.getChildAt(i);
            boolean isSel = (v == selected);
            v.setSelected(isSel);
            if (v instanceof Button) {
                applyTabAppearance((Button) v, isSel);
            }
        }
    }

    private void buildShopCatalog() {
        allItems.clear();

        // ═══════════════════════════════════════════
        // BUNDLE — Starter Bundle (Spans 2 columns)
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.starterBundle(this));

        // ═══════════════════════════════════════════
        // CONSUMABLES — Hints & Undos
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.hintPack3(this));
        allItems.add(ShopItem.hintPack10(this));
        allItems.add(ShopItem.undoPack3(this));

        // ═══════════════════════════════════════════
        // AVATAR FRAMES — 8 items
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "frame_default", getString(R.string.frame_default_title), getString(R.string.frame_default_desc),
            0, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.COMMON, "🪵"));

        allItems.add(ShopItem.permanent(
            "frame_bronze", getString(R.string.frame_bronze_title), getString(R.string.frame_bronze_desc),
            150, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.COMMON, "🥉"));

        allItems.add(ShopItem.withUnlock(
            "frame_silver", getString(R.string.frame_silver_title), getString(R.string.frame_silver_desc),
            400, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.RARE, "🥈", 10));

        allItems.add(ShopItem.withUnlock(
            "frame_carpet", getString(R.string.frame_carpet_title), getString(R.string.frame_carpet_desc),
            550, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.RARE, "🟥", 20));

        allItems.add(ShopItem.withUnlock(
            "frame_gold", getString(R.string.frame_gold_title), getString(R.string.frame_gold_desc),
            1000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.EPIC, "🥇", 50));

        allItems.add(ShopItem.withUnlock(
            "frame_peacock", getString(R.string.frame_peacock_title), getString(R.string.frame_peacock_desc),
            1500, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.EPIC, "🦚", 75));

        allItems.add(new ShopItem(
            "frame_diamond", getString(R.string.frame_diamond_title), getString(R.string.frame_diamond_desc),
            3000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "💎",
            0, 0, 150, getString(R.string.shop_badge_popular), 0, false));

        allItems.add(new ShopItem(
            "frame_sultan", getString(R.string.frame_sultan_title), getString(R.string.frame_sultan_desc),
            5000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "👑",
            0, 0, 300, null, 0, false));

        // ═══════════════════════════════════════════
        // DICE SKINS — 6 items
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "dice_default", getString(R.string.dice_default_title), getString(R.string.dice_default_desc),
            0, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON, "🎲"));

        allItems.add(ShopItem.permanent(
            "dice_walnut", getString(R.string.dice_walnut_title), getString(R.string.dice_walnut_desc),
            200, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON, "🟫"));

        allItems.add(ShopItem.withUnlock(
            "dice_ruby", getString(R.string.dice_ruby_title), getString(R.string.dice_ruby_desc),
            500, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE, "🔴", 15));

        allItems.add(ShopItem.withUnlock(
            "dice_marble", getString(R.string.dice_marble_title), getString(R.string.dice_marble_desc),
            700, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE, "⬜", 30));

        allItems.add(new ShopItem(
            "dice_crystal", getString(R.string.dice_crystal_title), getString(R.string.dice_crystal_desc),
            1400, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC, "🔷",
            0, 0, 100, getString(R.string.shop_badge_new), 0, false));

        allItems.add(new ShopItem(
            "dice_dragon", getString(R.string.dice_dragon_title), getString(R.string.dice_dragon_desc),
            3500, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.LEGENDARY, "🐉",
            0, 0, 200, null, 0, false));

        // ═══════════════════════════════════════════
        // TITLES — 6 items
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "title_beginner", getString(R.string.title_beginner_title), getString(R.string.title_beginner_desc),
            0, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "🌱"));

        allItems.add(ShopItem.withUnlock(
            "title_sharp", getString(R.string.title_sharp_title), getString(R.string.title_sharp_desc),
            100, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "🧩", 5));

        allItems.add(ShopItem.withUnlock(
            "title_tactician", getString(R.string.title_tactician_title), getString(R.string.title_tactician_desc),
            350, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "⚔️", 25));

        allItems.add(ShopItem.withUnlock(
            "title_master", getString(R.string.title_master_title), getString(R.string.title_master_desc),
            600, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "🎓", 60));

        allItems.add(new ShopItem(
            "title_king", getString(R.string.title_king_title), getString(R.string.title_king_desc),
            1200, ShopItem.Category.TITLE, ShopItem.Rarity.EPIC, "♔",
            0, 0, 100, null, 0, false));

        allItems.add(new ShopItem(
            "title_sultan", getString(R.string.title_sultan_title), getString(R.string.title_sultan_desc),
            2500, ShopItem.Category.TITLE, ShopItem.Rarity.LEGENDARY, "🏆",
            0, 0, 250, null, 0, false));
    }
}
