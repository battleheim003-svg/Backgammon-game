package games.mrlaki5.backgammon;

import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import games.mrlaki5.backgammon.Database.PlayerProfileManager;
import games.mrlaki5.backgammon.Retention.AchievementManager;

public class PlayerStatsActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player_stats);

        ImageButton btnBack = findViewById(R.id.btnStatsBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        bindStats();
    }

    private void bindStats() {
        PlayerProfileManager profile = PlayerProfileManager.getInstance(this);
        WinStreakTracker streakTracker = new WinStreakTracker(this);
        AchievementManager achievementManager = new AchievementManager(this);

        // Avatar frame emoji
        TextView tvAvatar = findViewById(R.id.tvStatsAvatar);
        if (tvAvatar != null) {
            tvAvatar.setText(frameEmoji(profile.getActiveFrame()));
        }

        // Player name
        TextView tvName = findViewById(R.id.tvStatsPlayerName);
        if (tvName != null) {
            tvName.setText(profile.getDisplayName());
        }

        // Active title
        TextView tvTitle = findViewById(R.id.tvStatsActiveTitle);
        if (tvTitle != null) {
            tvTitle.setText(titleLabel(profile.getActiveTitle()));
        }

        // Wins / Losses / Draws (draws = total - wins - losses)
        int wins = profile.getWins();
        int losses = profile.getLosses();
        int total = profile.getTotalGames();
        int draws = Math.max(0, total - wins - losses);
        TextView tvWLD = findViewById(R.id.tvStatsWLD);
        if (tvWLD != null) {
            tvWLD.setText(wins + " / " + losses + " / " + draws);
        }

        // Win rate
        TextView tvWinRate = findViewById(R.id.tvStatsWinRate);
        if (tvWinRate != null) {
            tvWinRate.setText(profile.getWinRate() + "%");
        }

        // Best streak
        TextView tvBestStreak = findViewById(R.id.tvStatsBestStreak);
        if (tvBestStreak != null) {
            tvBestStreak.setText(streakTracker.getBestStreak() + " 🔥");
        }

        // ELO
        TextView tvElo = findViewById(R.id.tvStatsElo);
        if (tvElo != null) {
            tvElo.setText(String.valueOf(profile.getElo()));
        }

        // Coin balance
        TextView tvCoins = findViewById(R.id.tvStatsCoins);
        if (tvCoins != null) {
            games.mrlaki5.backgammon.Economy.CoinManager coinManager =
                    new games.mrlaki5.backgammon.Economy.CoinManager(this);
            tvCoins.setText(coinManager.getBalance() + " 🪙");
        }

        // Achievements unlocked count
        TextView tvAchievements = findViewById(R.id.tvStatsAchievements);
        if (tvAchievements != null) {
            int unlocked = countUnlockedAchievements(achievementManager);
            tvAchievements.setText(unlocked + " / " + AchievementManager.ACHIEVEMENT_IDS.length);
        }
    }

    private int countUnlockedAchievements(AchievementManager manager) {
        int count = 0;
        for (String id : AchievementManager.ACHIEVEMENT_IDS) {
            if (manager.isUnlocked(id)) count++;
        }
        return count;
    }

    private String frameEmoji(String frameId) {
        if (frameId == null) return "🪵";
        switch (frameId) {
            case "frame_bronze":  return "🥉";
            case "frame_silver":  return "🥈";
            case "frame_carpet":  return "🟥";
            case "frame_gold":    return "🥇";
            case "frame_peacock": return "🦚";
            case "frame_diamond": return "💎";
            case "frame_sultan":  return "👑";
            default:              return "🪵";
        }
    }

    private String titleLabel(String titleId) {
        if (titleId == null) return getString(R.string.title_beginner_title);
        switch (titleId) {
            case "title_sharp":     return getString(R.string.title_sharp_title);
            case "title_tactician": return getString(R.string.title_tactician_title);
            case "title_master":    return getString(R.string.title_master_title);
            case "title_king":      return getString(R.string.title_king_title);
            case "title_sultan":    return getString(R.string.title_sultan_title);
            default:                return getString(R.string.title_beginner_title);
        }
    }
}
