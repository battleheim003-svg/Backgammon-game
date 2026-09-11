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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.LocaleHelper;
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
    private TextView tvCoinBalance;
    private RecyclerView rvShopItems;
    private ShopAdapter shopAdapter;
    private ShopItem.Category selectedCategory = ShopItem.Category.ALL;

    private final List<ShopItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_coin_shop);

        coinManager = new CoinManager(this);
        profileManager = PlayerProfileManager.getInstance(this);

        tvCoinBalance = findViewById(R.id.tvShopCoinBalance);
        rvShopItems = findViewById(R.id.shopRecyclerView);

        ImageButton btnBack = findViewById(R.id.btnShopBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        buildShopCatalog();
        setupRecyclerView();
        setupCategoryTabs();
        refreshCoinBalance();
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
                        return 2; // BUNDLE spans full 2 columns
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

        for (int i = 0; i < cats.length; i++) {
            final ShopItem.Category cat = cats[i];
            Button btn = new Button(new ContextThemeWrapper(this, R.style.ShopCategoryTabButton), null, 0);
            btn.setText(labelRes[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(16);
            btn.setLayoutParams(lp);

            if (i == 0) {
                btn.setSelected(true);
                btn.setBackgroundColor(0x33F4B044);
            }

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

    private void updateTabSelection(LinearLayout tabs, Button selected) {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            View v = tabs.getChildAt(i);
            boolean isSel = (v == selected);
            v.setSelected(isSel);
            if (isSel) {
                v.setBackgroundColor(0x33F4B044);
            } else {
                v.setBackgroundColor(0x00000000);
            }
        }
    }

    private void buildShopCatalog() {
        allItems.clear();

        // ═══════════════════════════════════════════
        // BUNDLE — Starter Bundle (Spans 2 columns)
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.starterBundle());

        // ═══════════════════════════════════════════
        // CONSUMABLES — Hints & Undos
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.hintPack3());
        allItems.add(ShopItem.hintPack10());
        allItems.add(ShopItem.undoPack3());

        // ═══════════════════════════════════════════
        // AVATAR FRAMES — 8 items
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "frame_default", "فریم کلاسیک", "حاشیه چوبی ساده",
            0, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.COMMON, "🪵"));

        allItems.add(ShopItem.permanent(
            "frame_bronze", "فریم برنز", "حاشیه برنز عتیقه صیقلی",
            150, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.COMMON, "🥉"));

        allItems.add(ShopItem.withUnlock(
            "frame_silver", "فریم نقره", "حاشیه نقره استرلینگ درخشان",
            400, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.RARE, "🥈", 10));

        allItems.add(ShopItem.withUnlock(
            "frame_carpet", "فریم فرش ایرانی", "نقوش سنتی فرش دستباف اصفهان",
            550, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.RARE, "🟥", 20));

        allItems.add(ShopItem.withUnlock(
            "frame_gold", "فریم طلا", "طلای ۲۴ عیار ایرانی با نقش شاهانه",
            1000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.EPIC, "🥇", 50));

        allItems.add(ShopItem.withUnlock(
            "frame_peacock", "فریم طاووس", "نقوش طاووس فارسی با جواهرات",
            1500, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.EPIC, "🦚", 75));

        allItems.add(new ShopItem(
            "frame_diamond", "تاج الماس", "تاج سلطنتی با الماسهای درخشان",
            3000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "💎", 150, false, "پرطرفدار"));

        allItems.add(new ShopItem(
            "frame_sultan", "فریم سلطان", "فریم ویژه سلاطین تختهنرد",
            5000, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "👑", 300, false, null));

        // ═══════════════════════════════════════════
        // DICE SKINS — 6 items
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "dice_default", "تاس استخوانی", "تاس کلاسیک سفید",
            0, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON, "🎲"));

        allItems.add(ShopItem.permanent(
            "dice_walnut", "تاس گردو", "تاس ماهونی تیره طبیعی",
            200, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.COMMON, "🟫"));

        allItems.add(ShopItem.withUnlock(
            "dice_ruby", "تاس یاقوت", "تاس یاقوت قرمز آتشین",
            500, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE, "🔴", 15));

        allItems.add(ShopItem.withUnlock(
            "dice_marble", "تاس مرمر", "تاس مرمر ابروباد زیبا",
            700, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.RARE, "⬜", 30));

        allItems.add(new ShopItem(
            "dice_crystal", "تاس کریستال", "تاس بلور شفاف با درخش نور",
            1400, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.EPIC, "🔷", 100, false, "جدید"));

        allItems.add(new ShopItem(
            "dice_dragon", "تاس اژدها", "تاس اسطورهای با نقش اژدها",
            3500, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.LEGENDARY, "🐉", 200, false, null));

        // ═══════════════════════════════════════════
        // TITLES — 6 items (Farsi)
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.permanent(
            "title_beginner", "نوآموز", "عنوان شروع بازیکن",
            0, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "🌱"));

        allItems.add(ShopItem.withUnlock(
            "title_sharp", "تیزهوش", "بازیکن باهوش تختهنرد",
            100, ShopItem.Category.TITLE, ShopItem.Rarity.COMMON, "🧩", 5));

        allItems.add(ShopItem.withUnlock(
            "title_tactician", "تاکتیسین", "خبره استراتژی و تاکتیک",
            350, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "⚔️", 25));

        allItems.add(ShopItem.withUnlock(
            "title_master", "استاد تخته", "استاد شناختهشده تختهنرد",
            600, ShopItem.Category.TITLE, ShopItem.Rarity.RARE, "🎓", 60));

        allItems.add(new ShopItem(
            "title_king", "شاهباز", "بازیکن سلطنتی تختهنرد",
            1200, ShopItem.Category.TITLE, ShopItem.Rarity.EPIC, "♔", 100, false, null));

        allItems.add(new ShopItem(
            "title_sultan", "سلطان تخته", "عنوان افسانهای — فقط برای نخبگان",
            2500, ShopItem.Category.TITLE, ShopItem.Rarity.LEGENDARY, "🏆", 250, false, null));

        // ═══════════════════════════════════════════
        // RENTALS — 24-hour try-before-you-buy
        // ═══════════════════════════════════════════
        allItems.add(ShopItem.rental(
            "rental_diamond", "تاج الماس — ۲۴ ساعت", "فریم افسانهای برای یک روز",
            50, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "💎"));

        allItems.add(ShopItem.rental(
            "rental_sultan", "فریم سلطان — ۲۴ ساعت", "فریم سلطانی برای یک روز",
            80, ShopItem.Category.AVATAR_FRAME, ShopItem.Rarity.LEGENDARY, "👑"));

        allItems.add(ShopItem.rental(
            "rental_dragon", "تاس اژدها — ۲۴ ساعت", "تاس افسانهای برای یک روز",
            60, ShopItem.Category.DICE_SKIN, ShopItem.Rarity.LEGENDARY, "🐉"));
    }
}