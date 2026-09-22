package games.mrlaki5.backgammon.Analysis;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.R;

/** Post-game review: every turn the player made, graded against the engine's best play. */
public class GameReviewActivity extends AppCompatActivity {

    private static final int COLOR_EXCELLENT = 0xFF4CAF50;
    private static final int COLOR_GOOD = 0xFF9CCC65;
    private static final int COLOR_MISTAKE = 0xFFFFB300;
    private static final int COLOR_BLUNDER = 0xFFE57373;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game_review);

        View back = findViewById(R.id.btnReviewBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        List<TurnAnalysis> analyses = GameReviewData.take();
        TextView summary = findViewById(R.id.tvReviewSummary);
        LinearLayout container = findViewById(R.id.reviewList);
        if (summary == null || container == null) {
            return;
        }

        if (analyses.isEmpty()) {
            summary.setText(R.string.review_empty);
            return;
        }

        summary.setText(getString(R.string.review_accuracy, GameAnalyzer.accuracy(analyses),
                analyses.size()));
        for (TurnAnalysis analysis : analyses) {
            container.addView(row(analysis));
        }
    }

    private View row(TurnAnalysis analysis) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding((int) (10 * density), (int) (8 * density),
                (int) (10 * density), (int) (8 * density));

        TextView header = new TextView(this);
        header.setText(getString(R.string.review_turn_header, analysis.getTurnNumber(),
                analysis.getDiceLabel(), analysis.getPlayedNotation()));
        header.setTextColor(Color.WHITE);
        header.setTextSize(14f);
        row.addView(header);

        TextView verdict = new TextView(this);
        verdict.setTextColor(gradeColor(analysis.getGrade()));
        verdict.setTextSize(13f);
        verdict.setGravity(Gravity.START);
        String label = getString(gradeLabel(analysis.getGrade()));
        verdict.setText(analysis.isBestPlay()
                ? label
                : getString(R.string.review_better_move, label, analysis.getBestNotation()));
        row.addView(verdict);
        return row;
    }

    private static int gradeColor(TurnAnalysis.Grade grade) {
        switch (grade) {
            case EXCELLENT: return COLOR_EXCELLENT;
            case GOOD: return COLOR_GOOD;
            case MISTAKE: return COLOR_MISTAKE;
            default: return COLOR_BLUNDER;
        }
    }

    private static int gradeLabel(TurnAnalysis.Grade grade) {
        switch (grade) {
            case EXCELLENT: return R.string.review_grade_excellent;
            case GOOD: return R.string.review_grade_good;
            case MISTAKE: return R.string.review_grade_mistake;
            default: return R.string.review_grade_blunder;
        }
    }
}
