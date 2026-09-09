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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_shop_card, parent, false);
        }

        ShopItem item = getItem(position);
        TextView tvIcon = convertView.findViewById(R.id.tvShopItemIcon);
        TextView tvTitle = convertView.findViewById(R.id.tvShopItemTitle);
        TextView tvDesc = convertView.findViewById(R.id.tvShopItemDesc);
        Button btnAction = convertView.findViewById(R.id.btnShopItemAction);

        tvIcon.setText(item.getIconEmoji());
        tvTitle.setText(item.getTitle());
        tvDesc.setText(item.getDescription());

        boolean isOwned = item.isFree() || profileManager.isItemPurchased(item.getId());
        boolean isEquipped = isItemEquipped(item);

        if (isEquipped) {
            btnAction.setText(R.string.shop_item_equipped);
            btnAction.setBackgroundResource(R.drawable.neuro_secondary_button);
            btnAction.setEnabled(false);
        } else if (isOwned) {
            btnAction.setText(R.string.shop_item_equip);
            btnAction.setBackgroundResource(R.drawable.neuro_secondary_button);
            btnAction.setEnabled(true);
            btnAction.setOnClickListener(v -> {
                equipItem(item);
                notifyDataSetChanged();
                if (actionListener != null) {
                    actionListener.onItemAction();
                }
            });
        } else {
            btnAction.setText(context.getString(R.string.shop_item_buy, item.getPrice()));
            btnAction.setBackgroundResource(R.drawable.neuro_primary_button);
            btnAction.setEnabled(true);
            btnAction.setOnClickListener(v -> {
                if (coinManager.spend(item.getPrice(), "shop_purchase_" + item.getId())) {
                    profileManager.addPurchasedItem(item.getId());
                    equipItem(item);
                    Toast.makeText(context, R.string.shop_purchase_success, Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    if (actionListener != null) {
                        actionListener.onItemAction();
                    }
                } else {
                    Toast.makeText(context, R.string.insufficient_coins, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return convertView;
    }

    private boolean isItemEquipped(ShopItem item) {
        switch (item.getCategory()) {
            case AVATAR_FRAME:
                return item.getId().equals(profileManager.getActiveFrame());
            case DICE_SKIN:
                return item.getId().equals(profileManager.getActiveDice());
            case TITLE:
                return item.getId().equals(profileManager.getActiveTitle());
            case THEME:
            default:
                return false;
        }
    }

    private void equipItem(ShopItem item) {
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
                break;
        }
    }
}
