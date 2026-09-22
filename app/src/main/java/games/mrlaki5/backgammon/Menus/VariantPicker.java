package games.mrlaki5.backgammon.Menus;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.royalbackgammon.core.variant.Variant;

import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.R;

/** Binds include_variant_picker: variant buttons, match-length cycler and rule hint. */
public final class VariantPicker {

    private static final Variant[] VARIANTS = {Variant.STANDARD, Variant.TAVLA, Variant.PORTES, Variant.NARDY};
    private static final int[] BUTTON_IDS = {R.id.variantStandard, R.id.variantTavla, R.id.variantPortes, R.id.variantNardy};
    private static final int[] MATCH_LENGTHS = {1, 3, 5, 7};

    private final Context context;
    private final Button[] variantButtons = new Button[VARIANTS.length];
    private final Button matchButton;
    private final TextView hint;
    private Variant variant;
    private int matchLength;

    public VariantPicker(View root) {
        context = root.getContext();
        variant = GamePreferences.getVariant(context);
        matchLength = GamePreferences.getMatchLength(context);

        for (int i = 0; i < VARIANTS.length; i++) {
            final Variant v = VARIANTS[i];
            variantButtons[i] = root.findViewById(BUTTON_IDS[i]);
            variantButtons[i].setOnClickListener(btn -> {
                variant = v;
                render();
            });
        }
        matchButton = root.findViewById(R.id.matchLength);
        matchButton.setOnClickListener(btn -> {
            matchLength = nextMatchLength(matchLength);
            render();
        });
        hint = root.findViewById(R.id.variantHint);
        render();
    }

    /** Persists the selection and adds variant/match extras for GameActivity. */
    public void applyTo(Intent intent) {
        GamePreferences.saveVariantSelection(context, variant, matchLength);
        intent.putExtra(MenuActivity.EXTRA_VARIANT, variant.name());
        intent.putExtra(MenuActivity.EXTRA_MATCH_TARGET, matchLength);
    }

    private void render() {
        for (int i = 0; i < VARIANTS.length; i++) {
            boolean selected = VARIANTS[i] == variant;
            variantButtons[i].setBackgroundResource(selected
                    ? R.drawable.bg_difficulty_selected : R.drawable.bg_difficulty_normal);
            variantButtons[i].setTextColor(selected ? 0xFFE6A100 : 0xFF7A8A99);
        }
        matchButton.setText(matchLabel(context, matchLength));
        matchButton.setBackgroundResource(matchLength > 1
                ? R.drawable.bg_difficulty_selected : R.drawable.bg_difficulty_normal);
        matchButton.setTextColor(matchLength > 1 ? 0xFFE6A100 : 0xFF7A8A99);
        hint.setText(ruleSummaryRes(variant));
    }

    private static int nextMatchLength(int current) {
        for (int i = 0; i < MATCH_LENGTHS.length; i++) {
            if (MATCH_LENGTHS[i] == current) {
                return MATCH_LENGTHS[(i + 1) % MATCH_LENGTHS.length];
            }
        }
        return MATCH_LENGTHS[0];
    }

    public static String matchLabel(Context context, int length) {
        return length <= 1
                ? context.getString(R.string.match_single)
                : context.getString(R.string.match_to_points, length);
    }

    public static int nameRes(Variant variant) {
        switch (variant) {
            case TAVLA: return R.string.variant_tavla;
            case PORTES: return R.string.variant_portes;
            case NARDY: return R.string.variant_nardy;
            default: return R.string.variant_standard;
        }
    }

    private static int ruleSummaryRes(Variant variant) {
        switch (variant) {
            case TAVLA: return R.string.variant_tavla_rules;
            case PORTES: return R.string.variant_portes_rules;
            case NARDY: return R.string.variant_nardy_rules;
            default: return R.string.variant_standard_rules;
        }
    }
}
