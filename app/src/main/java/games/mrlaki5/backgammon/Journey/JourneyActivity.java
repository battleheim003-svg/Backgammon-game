package games.mrlaki5.backgammon.Journey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.GameControllers.GameActivity;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Menus.VariantPicker;
import games.mrlaki5.backgammon.R;

/** The journey: themed chapters that unlock one by one and teach every variant. */
public class JourneyActivity extends AppCompatActivity {

    private static final int COLOR_DONE = 0xFF4CAF50;
    private static final int COLOR_OPEN = 0xFFE6A100;
    private static final int COLOR_LOCKED = 0xFF7A8A99;

    private JourneyManager journeyManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_journey);
        journeyManager = new JourneyManager(this);

        View back = findViewById(R.id.btnJourneyBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindStages();
    }

    private void bindStages() {
        LinearLayout list = findViewById(R.id.journeyList);
        TextView progress = findViewById(R.id.tvJourneyProgress);
        if (list == null) return;
        list.removeAllViews();

        JourneyStage[] stages = JourneyStage.values();
        if (progress != null) {
            progress.setText(getString(R.string.journey_progress,
                    journeyManager.getCompletedCount(), stages.length));
        }
        for (int i = 0; i < stages.length; i++) {
            list.addView(stageRow(i, stages[i]));
        }
    }

    private View stageRow(int index, JourneyStage stage) {
        float density = getResources().getDisplayMetrics().density;
        boolean unlocked = journeyManager.isUnlocked(index);
        boolean completed = journeyManager.isCompleted(index);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding((int) (12 * density), (int) (10 * density),
                (int) (12 * density), (int) (10 * density));
        row.setBackgroundResource(unlocked
                ? R.drawable.bg_difficulty_selected : R.drawable.bg_difficulty_normal);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (8 * density);
        row.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(getString(R.string.journey_stage_title, index + 1, getString(stage.getTitleRes())));
        title.setTextColor(completed ? COLOR_DONE : (unlocked ? COLOR_OPEN : COLOR_LOCKED));
        title.setTextSize(15f);
        row.addView(title);

        TextView detail = new TextView(this);
        detail.setText(getString(R.string.journey_stage_detail,
                getString(stage.getOpponentRes()),
                getString(stage.isTavliRotation()
                        ? R.string.variant_tavli : VariantPicker.nameRes(stage.getVariant())),
                VariantPicker.matchLabel(this, stage.getMatchTarget()),
                stage.getRewardCoins()));
        detail.setTextColor(0xFFCFD8DC);
        detail.setTextSize(12f);
        row.addView(detail);

        TextView flavor = new TextView(this);
        flavor.setText(unlocked ? getString(stage.getFlavorRes()) : getString(R.string.journey_locked));
        flavor.setTextColor(COLOR_LOCKED);
        flavor.setTextSize(11f);
        row.addView(flavor);

        if (unlocked) {
            row.setOnClickListener(v -> startStage(index, stage));
        } else {
            row.setOnClickListener(v ->
                    Toast.makeText(this, R.string.journey_locked, Toast.LENGTH_SHORT).show());
        }
        return row;
    }

    private void startStage(int index, JourneyStage stage) {
        PlayerProfileManager profile = PlayerProfileManager.getInstance(this);
        java.io.File save = new java.io.File(getFilesDir().getAbsolutePath(),
                MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        save.delete();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(MenuActivity.EXTRA_PLAYER1_NAME, profile.getDisplayName());
        intent.putExtra(MenuActivity.EXTRA_PLAYER2_NAME, getString(stage.getOpponentRes()));
        intent.putExtra(MenuActivity.EXTRA_PLAYER1_KIND, "Player");
        intent.putExtra(MenuActivity.EXTRA_PLAYER2_KIND, "Bot");
        intent.putExtra(MenuActivity.EXTRA_VARIANT, stage.getVariant().name());
        intent.putExtra(MenuActivity.EXTRA_MATCH_TARGET, stage.getMatchTarget());
        intent.putExtra(MenuActivity.EXTRA_TAVLI, stage.isTavliRotation());
        intent.putExtra(MenuActivity.EXTRA_BOT_DIFFICULTY, stage.getDifficulty());
        intent.putExtra(MenuActivity.EXTRA_JOURNEY_STAGE, index);
        startActivity(intent);
    }
}
