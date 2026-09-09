package games.mrlaki5.backgammon.Economy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.R;

/**
 * Dialog displaying the 7-day daily login bonus calendar and claim action.
 */
public class DailyLoginDialog {

    private final Activity activity;
    private final DailyLoginManager loginManager;
    private final CoinManager coinManager;
    private final Runnable onClaimedCallback;
    private AlertDialog dialog;

    public DailyLoginDialog(Activity activity, DailyLoginManager loginManager,
                            CoinManager coinManager, Runnable onClaimedCallback) {
        this.activity = activity;
        this.loginManager = loginManager;
        this.coinManager = coinManager;
        this.onClaimedCallback = onClaimedCallback;
    }

    public void show() {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_daily_login, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(true);
        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.75f);
        }

        int currentStreak = loginManager.getCurrentStreak();
        boolean canClaim = loginManager.canClaimToday();

        // Calculate which day card corresponds to today (1..7)
        int todayDay = canClaim ? (currentStreak % 7) + 1 : ((currentStreak - 1) % 7) + 1;

        int[] cardIds = new int[]{
                R.id.dayCard1, R.id.dayCard2, R.id.dayCard3,
                R.id.dayCard4, R.id.dayCard5, R.id.dayCard6, R.id.dayCard7
        };

        for (int i = 0; i < cardIds.length; i++) {
            LinearLayout card = dialogView.findViewById(cardIds[i]);
            if (card != null) {
                int dayNumber = i + 1;
                if (dayNumber < todayDay) {
                    card.setAlpha(0.5f);
                } else if (dayNumber == todayDay) {
                    card.setScaleX(1.08f);
                    card.setScaleY(1.08f);
                    card.setBackgroundResource(R.drawable.neuro_secondary_button);
                } else {
                    card.setAlpha(0.85f);
                }
            }
        }

        Button btnClaim = dialogView.findViewById(R.id.btnDailyClaim);
        Button btnClose = dialogView.findViewById(R.id.btnDailyClose);

        if (canClaim) {
            btnClaim.setText(R.string.daily_login_claim);
            btnClaim.setEnabled(true);
            btnClaim.setOnClickListener(v -> {
                int reward = loginManager.claimDailyReward();
                if (reward > 0) {
                    btnClaim.setText(R.string.daily_login_claimed);
                    btnClaim.setEnabled(false);
                    Toast.makeText(activity, activity.getString(R.string.coin_earned, reward), Toast.LENGTH_SHORT).show();
                    if (onClaimedCallback != null) {
                        onClaimedCallback.run();
                    }
                    dialogView.postDelayed(() -> {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }, 1200);
                }
            });
        } else {
            btnClaim.setText(R.string.daily_login_claimed);
            btnClaim.setEnabled(false);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
