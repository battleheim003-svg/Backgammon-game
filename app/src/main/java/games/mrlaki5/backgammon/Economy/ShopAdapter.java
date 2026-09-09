package games.mrlaki5.backgammon.Economy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.GameView.themes.BoardThemeFactory;
import games.mrlaki5.backgammon.R;

/**
 * Adapter for rendering shop item cards with buy/equip state handling.
 */
public class ShopAdapter extends BaseAdapter {

    public interface OnItemActionListener {
        void onItemAction();
    }

    private final Context context;
    private final List<ShopItem> items;
    private final PlayerProfileManager profileManager;
    private final CoinManager coinManager;
    private final OnItemActionListener actionListener;

    public ShopAdapter(Context context, List<ShopItem> items,
                       PlayerProfileManager profileManager, CoinManager coinManager,
                       OnItemActionListener listener) {
        this.context = context;
        this.items = items;
        this.profileManager = profileManager;
        this.coinManager = coinManager;
        this.actionListener = listener;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ShopItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_shop_card, parent, false);
        }

        ShopItem item = getItem(position);

        // Find views
        TextView tvIcon      = convertView.findViewById(R.id.tvShopItemIcon);
        TextView tvTitle     = convertView.findViewById(R.id.tvShopItemTitle);
        TextView tvDesc      = convertView.findViewById(R.id.tvShopItemDesc);
        TextView tvRarity    = convertView.findViewById(R.id.tvShopItemRarity);
        TextView tvBadge     = convertView.findViewById(R.id.tvShopItemBadge);
        TextView tvUnlock    = convertView.findViewById(R.id.tvShopItemUnlock);
        View     rarityBar   = convertView.findViewById(R.id.viewRarityBar);
        Button   btnAction   = convertView.findViewById(R.id.btnShopItemAction);

        // Content
        tvIcon.setText(item.getIconEmoji());
        tvTitle.setText(item.getTitle());
        tvDesc.setText(item.getDescription());

        // Rarity
        int rarityColor = android.graphics.Color.parseColor(item.getRarity().hexColor());
        tvRarity.setText(item.getRarity().label());
        tvRarity.setTextColor(rarityColor);
        if (rarityBar != null) rarityBar.setBackgroundColor(rarityColor);

        // Badge
        if (tvBadge != null) {
            if (item.getBadge() != null) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(item.getBadge());
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }

        // Unlock requirement
        int playerWins = profileManager.getTotalWins();
        boolean isLocked = item.hasUnlockReq() && playerWins < item.getUnlockRequirement();
        if (tvUnlock != null) {
            if (isLocked) {
                tvUnlock.setVisibility(View.VISIBLE);
                tvUnlock.setText("🔒 نیاز به " + item.getUnlockRequirement() + " برد");
            } else {
                tvUnlock.setVisibility(View.GONE);
            }
        }

        // Owned / equip state
        boolean isOwned   = item.isFree() || profileManager.isItemPurchased(item.getId());
        boolean isEquipped = !isLocked && isItemEquipped(item);

        if (isLocked) {
            btnAction.setText("قفل");
            btnAction.setEnabled(false);
            btnAction.setAlpha(0.5f);
        } else if (isEquipped) {
            btnAction.setText(R.string.shop_item_equipped);
            btnAction.setBackgroundResource(R.drawable.neuro_secondary_button);
            btnAction.setEnabled(false);
            btnAction.setAlpha(1f);
        } else if (isOwned) {
            btnAction.setText(R.string.shop_item_equip);
            btnAction.setBackgroundResource(R.drawable.neuro_secondary_button);
            btnAction.setEnabled(true);
            btnAction.setAlpha(1f);
            btnAction.setOnClickListener(v -> {
                equipItem(item);
                notifyDataSetChanged();
                if (actionListener != null) actionListener.onItemAction();
            });
        } else {
            // Rental vs purchase label
            String priceLabel = item.isRental()
                    ? "اجاره " + item.getPrice() + " 🪙"
                    : item.getPrice() + " 🪙";
            btnAction.setText(priceLabel);
            btnAction.setBackgroundResource(R.drawable.neuro_primary_button);
            btnAction.setEnabled(true);
            btnAction.setAlpha(1f);
            btnAction.setOnClickListener(v -> {
                if (coinManager.spend(item.getPrice(), "shop_" + item.getId())) {
                    profileManager.addPurchasedItem(item.getId());
                    equipItem(item);
                    Toast.makeText(context, R.string.shop_purchase_success, Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    if (actionListener != null) actionListener.onItemAction();
                } else {
                    Toast.makeText(context, R.string.insufficient_coins, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return convertView;
    }

    private boolean isItemEquipped(ShopItem item) {
        if (item.isRental() || item.getCategory() == ShopItem.Category.RENTAL) {
            String targetId = item.getId().replace("rental_", "");
            if (item.getId().startsWith("rental_theme_") || item.getId().equals("rental_cyberpunk") || item.getId().equals("rental_luxury")) {
                String themeTarget = targetId.startsWith("theme_") ? targetId : "theme_" + targetId;
                return themeTarget.equals(profileManager.getActiveTheme());
            } else if (item.getId().startsWith("rental_frame_") || item.getId().equals("rental_diamond") || item.getId().equals("rental_sultan")) {
                String frameTarget = targetId.startsWith("frame_") ? targetId : "frame_" + targetId;
                return frameTarget.equals(profileManager.getActiveFrame());
            } else if (item.getId().startsWith("rental_dice_") || item.getId().equals("rental_dragon")) {
                String diceTarget = targetId.startsWith("dice_") ? targetId : "dice_" + targetId;
                return diceTarget.equals(profileManager.getActiveDice());
            }
        }
        switch (item.getCategory()) {
            case AVATAR_FRAME:
                return item.getId().equals(profileManager.getActiveFrame());
            case DICE_SKIN:
                return item.getId().equals(profileManager.getActiveDice());
            case TITLE:
                return item.getId().equals(profileManager.getActiveTitle());
            case THEME:
                return item.getId().equals(profileManager.getActiveTheme());
            default:
                return false;
        }
    }

    private void equipItem(ShopItem item) {
        if (item.isRental() || item.getCategory() == ShopItem.Category.RENTAL) {
            String targetId = item.getId().replace("rental_", "");
            if (item.getId().startsWith("rental_theme_") || item.getId().equals("rental_cyberpunk") || item.getId().equals("rental_luxury")) {
                String themeTarget = targetId.startsWith("theme_") ? targetId : "theme_" + targetId;
                profileManager.setActiveTheme(themeTarget);
                int themeId = BoardThemeFactory.themeIdFromString(themeTarget);
                int difficulty = GamePreferences.getBotDifficulty(context);
                GamePreferences.saveSelections(context, difficulty, themeId);
            } else if (item.getId().startsWith("rental_frame_") || item.getId().equals("rental_diamond") || item.getId().equals("rental_sultan")) {
                String frameTarget = targetId.startsWith("frame_") ? targetId : "frame_" + targetId;
                profileManager.setActiveFrame(frameTarget);
            } else if (item.getId().startsWith("rental_dice_") || item.getId().equals("rental_dragon")) {
                String diceTarget = targetId.startsWith("dice_") ? targetId : "dice_" + targetId;
                profileManager.setActiveDice(diceTarget);
            }
            return;
        }
        switch (item.getCategory()) {
            case AVATAR_FRAME:
                profileManager.setActiveFrame(item.getId());
                break;
            case DICE_SKIN:
                profileManager.setActiveDice(item.getId());
                break;
            case TITLE:
                profileManager.setActiveTitle(item.getId());
                break;
            case THEME:
                profileManager.setActiveTheme(item.getId());
                int themeId = BoardThemeFactory.themeIdFromString(item.getId());
                int difficulty = GamePreferences.getBotDifficulty(context);
                GamePreferences.saveSelections(context, difficulty, themeId);
                break;
        }
    }
}
