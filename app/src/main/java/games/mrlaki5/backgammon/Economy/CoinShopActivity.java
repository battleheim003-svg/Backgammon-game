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
        bindCollection();
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
                ShopItem.Category.CHECKERS,
                ShopItem.Category.DICE_SKIN,
                ShopItem.Category.TITLE,
                ShopItem.Category.COSMETIC,
                ShopItem.Category.CONSUMABLE,
                ShopItem.Category.BUNDLE
        };
        int[] labelRes = {
                R.string.shop_category_all,
                R.string.shop_category_checkers,
                R.string.shop_category_dice,
                R.string.shop_category_title,
                R.string.shop_category_cosmetic,
                R.string.shop_category_consumable,
                R.string.shop_category_bundle
        };

        for (int i = 0; i < cats.length; i++) {
            final ShopItem.Category cat = cats[i];
            Button btn = new Button(new ContextThemeWrapper(this, R.style.ShopCategoryTabButton), null, 0);
            btn.setText(getString(labelRes[i]));
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
                bindCollection();
            });
            tabs.addView(btn);
        }
    }

    /**
     * How much of the current shelf the player already holds.
     *
     * A list of prices is a catalogue; a count of what you own turns the same
     * list into a set worth completing, which is the whole point of collections.
     */
    private void bindCollection() {
        android.widget.TextView label = findViewById(R.id.tvShopCollectionLabel);
        android.widget.TextView count = findViewById(R.id.tvShopCollectionCount);
        android.widget.ProgressBar bar = findViewById(R.id.shopCollectionProgress);
        if (label == null || count == null || bar == null || allItems == null) {
            return;
        }

        games.mrlaki5.backgammon.Database.PlayerProfileManager profile =
                games.mrlaki5.backgammon.Database.PlayerProfileManager.getInstance(this);

        int total = 0;
        int owned = 0;
        for (ShopItem item : allItems) {
            if (item.isConsumable()) {
                continue;  // stock, not something you collect
            }
            if (selectedCategory != ShopItem.Category.ALL
                    && item.getCategory() != selectedCategory) {
                continue;
            }
            total++;
            if (item.isFree() || profile.isItemPurchased(item.getId())) {
                owned++;
            }
        }

        label.setText(getString(R.string.shop_collection_label));
        count.setText(getString(R.string.shop_collection_count, owned, total));
        bar.setProgress(total == 0 ? 0 : Math.round(owned * 100F / total));
        View container = findViewById(R.id.shopCollectionBar);
        if (container != null) {
            container.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
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
        // CHECKERS — the pieces the player actually touches
        //
        // Named for what a real set is made of rather than for a tier: a
        // bronze/silver/gold/diamond ladder belongs to every mobile game, and
        // walnut, camel bone, Isfahan khatam and Neyshabur turquoise belong to
        // this one. Each has its own rendered artwork.
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.crafted(
            "checkers_walnut", getString(R.string.checkers_walnut_title),
            getString(R.string.checkers_walnut_desc), 0,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.COMMON,
            R.drawable.checkers_walnut, 0));

        allItems.add(ShopItem.crafted(
            "checkers_bone", getString(R.string.checkers_bone_title),
            getString(R.string.checkers_bone_desc), 300,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.COMMON,
            R.drawable.checkers_bone, 0));

        allItems.add(ShopItem.crafted(
            "checkers_khatam", getString(R.string.checkers_khatam_title),
            getString(R.string.checkers_khatam_desc), 800,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.RARE,
            R.drawable.checkers_khatam, 15));

        allItems.add(ShopItem.crafted(
            "checkers_nacre", getString(R.string.checkers_nacre_title),
            getString(R.string.checkers_nacre_desc), 1100,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.RARE,
            R.drawable.checkers_nacre, 25));

        allItems.add(ShopItem.crafted(
            "checkers_turquoise", getString(R.string.checkers_turquoise_title),
            getString(R.string.checkers_turquoise_desc), 2000,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.EPIC,
            R.drawable.checkers_turquoise, 50));

        allItems.add(ShopItem.crafted(
            "checkers_agate", getString(R.string.checkers_agate_title),
            getString(R.string.checkers_agate_desc), 2800,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.EPIC,
            R.drawable.checkers_agate, 80));

        allItems.add(ShopItem.crafted(
            "checkers_gold", getString(R.string.checkers_gold_title),
            getString(R.string.checkers_gold_desc), 6000,
            ShopItem.Category.CHECKERS, ShopItem.Rarity.LEGENDARY,
            R.drawable.checkers_gold, 200));

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
        // DICE — cut from the stones a real pair is cut from
        //
        // Each is rendered as a cube in axonometric projection with its pips
        // drilled rather than printed, so what the shelf shows is the object
        // the player will be rolling.
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.crafted(
            "dice_bone", getString(R.string.dice_bone_title), getString(R.string.dice_bone_desc),
            0, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON,
            R.drawable.dice_set_bone, 0));

        allItems.add(ShopItem.crafted(
            "dice_walnut", getString(R.string.dice_walnut_title), getString(R.string.dice_walnut_desc),
            250, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON,
            R.drawable.dice_set_walnut, 0));

        allItems.add(ShopItem.crafted(
            "dice_ebony", getString(R.string.dice_ebony_title), getString(R.string.dice_ebony_desc),
            700, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE,
            R.drawable.dice_set_ebony, 12));

        allItems.add(ShopItem.crafted(
            "dice_turquoise", getString(R.string.dice_turquoise_title), getString(R.string.dice_turquoise_desc),
            1800, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC,
            R.drawable.dice_set_turquoise, 40));

        allItems.add(ShopItem.crafted(
            "dice_agate", getString(R.string.dice_agate_title), getString(R.string.dice_agate_desc),
            2400, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC,
            R.drawable.dice_set_agate, 60));

        allItems.add(ShopItem.crafted(
            "dice_gold", getString(R.string.dice_gold_title), getString(R.string.dice_gold_desc),
            5000, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.LEGENDARY,
            R.drawable.dice_set_gold, 150));

        // ═══════════════════════════════════════════
        // TITLES — earned in the language of the game itself
        //
        // Not a rank ladder. These are the things backgammon players actually
        // call each other: the one who builds a six-prime, the one whose doubles
        // keep coming, the one who wins by a gammon.
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "title_none", getString(R.string.title_none_title), getString(R.string.title_none_desc),
            0, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, ""));

        allItems.add(ShopItem.withUnlock(
            "title_doubler", getString(R.string.title_doubler_title), getString(R.string.title_doubler_desc),
            400, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "", 10));

        allItems.add(ShopItem.withUnlock(
            "title_prime", getString(R.string.title_prime_title), getString(R.string.title_prime_desc),
            900, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "", 25));

        allItems.add(ShopItem.withUnlock(
            "title_gammon", getString(R.string.title_gammon_title), getString(R.string.title_gammon_desc),
            1500, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "", 40));

        allItems.add(ShopItem.withUnlock(
            "title_plakoto", getString(R.string.title_plakoto_title), getString(R.string.title_plakoto_desc),
            2200, ShopItem.Category.TITLE, ShopItem.Rarity.EPIC, "", 70));

        allItems.add(ShopItem.withUnlock(
            "title_unbeaten", getString(R.string.title_unbeaten_title), getString(R.string.title_unbeaten_desc),
            4000, ShopItem.Category.TITLE, ShopItem.Rarity.LEGENDARY, "", 150));

    }
}
