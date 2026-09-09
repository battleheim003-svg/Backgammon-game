package games.mrlaki5.backgammon.GameControllers;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import games.mrlaki5.backgammon.Analytics.GameAnalytics;
import games.mrlaki5.backgammon.Database.DbHelper;
import games.mrlaki5.backgammon.Database.GameResultRecorder;
import games.mrlaki5.backgammon.Database.ScoresTableEntry;
import games.mrlaki5.backgammon.Economy.CoinConfig;
import games.mrlaki5.backgammon.Economy.CoinManager;
import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Monetization.ads.AdManager;
import games.mrlaki5.backgammon.R;
import games.mrlaki5.backgammon.Retention.AchievementManager;
import games.mrlaki5.backgammon.Retention.DailyChallenge;
import games.mrlaki5.backgammon.Retention.ReviewPromptManager;
import games.mrlaki5.backgammon.Retention.WeeklyChallenge;
import games.mrlaki5.backgammon.Util.DateUtil;
import games.mrlaki5.backgammon.WinStreakTracker;

/**
 * Handles game finish recording, coin economy rewards, win streaks, ELO updates,
 * analytics, ads, and game-over UI dialog.
 */
public class GameOverHandler {

    public interface OnGameOverActionListener {
        void onPlayEffect(int effectId);
        void onRematch();
        void onChangeSettings(int winningPlayer, String p1Name, String p2Name, String gameMode);
        void onMainMenu(int winningPlayer, String p1Name, String p2Name, String gameMode);
    }

    private final Activity activity;
    private final CoinManager coinManager;
    private AlertDialog gameOverDialog;
    private boolean gameResultRecorded = false;
    private OnGameOverActionListener listener;

    public GameOverHandler(Activity activity, CoinManager coinManager) {
        this.activity = activity;
        this.coinManager = coinManager;
    }

    public void setListener(OnGameOverActionListener listener) {
        this.listener = listener;
    }

    public boolean isResultRecorded() {
        return gameResultRecorded;
    }

    /**
     * Handles complete game over flow: stats recording, economy rewards, ads, dialog display.
     */
    public void handleGameFinished(int winningPlayer, String p1Name, String p2Name, String gameMode,
                                   boolean passAndPlayMode, boolean tutorialMode, boolean isRematchGame,
                                   int sessionGameNumber, long durationSeconds, String difficultyName) {
        if (!gameResultRecorded) {
            gameResultRecorded = true;
            recordGameResult(winningPlayer, p1Name, p2Name, gameMode);

            AdManager adMgr = MenuActivity.getSharedAdManager();
            if (adMgr != null) {
                adMgr.onGameCompleted();
            }
        }

        // Track analytics
        String winner = (winningPlayer == 1) ? "player" : "opponent";
        if (passAndPlayMode) {
            winner = (winningPlayer == 1) ? "player1" : "player2";
        }
        GameAnalytics.get().trackGameCompleted(gameMode, winner, durationSeconds,
                sessionGameNumber, 0, 0);

        if (!passAndPlayMode && !tutorialMode) {
            if (winningPlayer == 1) {
                GameAnalytics.get().trackGameWon(gameMode, difficultyName, durationSeconds);
            } else {
                GameAnalytics.get().trackGameLost(gameMode, difficultyName, durationSeconds);
            }
        }

        if (isRematchGame) {
            GameAnalytics.get().trackRematchCompleted(gameMode, difficultyName, winner);
        }

        // Delete save file
        File saveFile = new File(activity.getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        saveFile.delete();

        // Update win streak and coin rewards
        int currentStreak = 0;
        int coinsEarned = 0;
        if (!passAndPlayMode && !tutorialMode) {
            WinStreakTracker streakTracker = new WinStreakTracker(activity);
            int difficulty = GamePreferences.getBotDifficulty(activity);
            if (winningPlayer == 1) {
                currentStreak = streakTracker.recordWin();
                coinsEarned = CoinConfig.WIN_BASE;
                if (difficulty == 1) coinsEarned += CoinConfig.WIN_BONUS_MEDIUM;
                else if (difficulty == 2) coinsEarned += CoinConfig.WIN_BONUS_HARD;
                else if (difficulty == 3) coinsEarned += CoinConfig.WIN_BONUS_ROYAL;
                if (coinManager != null) {
                    coinManager.earn(coinsEarned, "game_win");
                }

                // Check first game of the day
                SharedPreferences flowPrefs = activity.getSharedPreferences("game_flow_prefs", Context.MODE_PRIVATE);
                int today = DateUtil.getDayOfYear();
                if (today != flowPrefs.getInt("last_game_day", -1)) {
                    if (coinManager != null) {
                        coinManager.earn(CoinConfig.FIRST_GAME_OF_DAY, "first_game_of_day");
                    }
                    flowPrefs.edit().putInt("last_game_day", today).apply();
                }

                // Streak milestone check
                if (currentStreak > 0 && currentStreak % CoinConfig.STREAK_MILESTONE_EVERY == 0) {
                    if (coinManager != null) {
                        coinManager.earn(CoinConfig.STREAK_MILESTONE_REWARD, "streak_milestone");
                    }
                }
            } else {
                streakTracker.recordLoss();
            }

            // Update achievements
            AchievementManager achievements = new AchievementManager(activity, coinManager);
            achievements.onGameCompleted(winningPlayer == 1, difficulty, currentStreak);

            // Update daily challenge
            DailyChallenge dailyChallenge = new DailyChallenge(activity, coinManager);
            dailyChallenge.onGameCompleted(winningPlayer == 1, difficulty, false, currentStreak);

            // Update weekly challenge
            WeeklyChallenge weeklyChallenge = new WeeklyChallenge(activity);
            weeklyChallenge.onGameCompleted(winningPlayer == 1);
        }

        final String winnerName = (winningPlayer == 1) ? p1Name : p2Name;
        final int streak = currentStreak;

        activity.runOnUiThread(() -> {
            showInterstitialIfAllowed();
            showGameOverDialog(winnerName, winningPlayer, p1Name, p2Name, gameMode,
                    passAndPlayMode, tutorialMode, streak, durationSeconds, sessionGameNumber, difficultyName);
        });
    }

    private void recordGameResult(int winningPlayer, String p1Name, String p2Name, String gameMode) {
        // Legacy database
        try {
            DbHelper helper = new DbHelper(activity);
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ScoresTableEntry.COLUMN_PLAYER1_NAME, p1Name);
            values.put(ScoresTableEntry.COLUMN_PLAYER2_NAME, p2Name);
            if (winningPlayer == 1) {
                values.put(ScoresTableEntry.COLUMN_PLAYER1_WIN, 1);
                values.put(ScoresTableEntry.COLUMN_PLAYER2_WIN, 0);
            } else {
                values.put(ScoresTableEntry.COLUMN_PLAYER1_WIN, 0);
                values.put(ScoresTableEntry.COLUMN_PLAYER2_WIN, 1);
            }
            Date currDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US);
            values.put(ScoresTableEntry.COLUMN_END_GAME_TIME, format.format(currDate));
            db.insert(ScoresTableEntry.TABLE_NAME, null, values);
        } catch (Exception e) {
            GameAnalytics.get().reportError(e, "recordGameResult_legacy");
        }

        // New ELO/profile system
        try {
            GameResultRecorder recorder = new GameResultRecorder(activity);
            String winnerName = (winningPlayer == 1) ? p1Name : p2Name;
            String loserName = (winningPlayer == 1) ? p2Name : p1Name;
            recorder.recordResult(winnerName, loserName, gameMode);
        } catch (Exception e) {
            GameAnalytics.get().reportError(e, "recordGameResult_elo");
        }
    }

    private void showGameOverDialog(String winnerName, int winningPlayer,
                                    String p1Name, String p2Name, String gameMode,
                                    boolean passAndPlayMode, boolean tutorialMode,
                                    int winStreak, long durationSec, int sessionGameNumber,
                                    String difficultyName) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (gameOverDialog != null && gameOverDialog.isShowing()) return;

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_game_over, null);

        // Set winner text
        TextView winnerText = dialogView.findViewById(R.id.gameOverWinner);
        if (winnerText != null) {
            winnerText.setText(activity.getString(R.string.game_over_winner, winnerName));
        }

        // Set game duration
        TextView durationText = dialogView.findViewById(R.id.gameOverDuration);
        if (durationText != null) {
            int minutes = (int) (durationSec / 60);
            int seconds = (int) (durationSec % 60);
            String durationStr = activity.getString(R.string.game_over_duration, minutes, seconds);
            if (sessionGameNumber > 1) {
                durationStr += "  •  " + activity.getString(R.string.game_over_game_number, sessionGameNumber);
            }
            durationText.setText(durationStr);
        }

        // Show win streak or daily challenge
        TextView streakText = dialogView.findViewById(R.id.gameOverStreak);
        if (streakText != null) {
            if (!passAndPlayMode && !tutorialMode && winningPlayer == 1 && winStreak >= 2) {
                streakText.setVisibility(View.VISIBLE);
                streakText.setText(activity.getString(R.string.game_over_win_streak, winStreak));
            } else if (!passAndPlayMode && !tutorialMode) {
                DailyChallenge dc = new DailyChallenge(activity);
                if (dc.isCompleted()) {
                    streakText.setVisibility(View.VISIBLE);
                    streakText.setText(activity.getString(R.string.daily_challenge_done));
                } else {
                    streakText.setVisibility(View.GONE);
                }
            } else {
                streakText.setVisibility(View.GONE);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(false);
        gameOverDialog = builder.create();

        if (gameOverDialog.getWindow() != null) {
            gameOverDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            gameOverDialog.getWindow().setDimAmount(0.75f);
        }

        // Rematch button
        View btnRematch = dialogView.findViewById(R.id.gameOverRematch);
        if (btnRematch != null) {
            btnRematch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayEffect(GameAudio.EFFECT_MENU_TAP);
                }
                GameAnalytics.get().trackRematchClicked(gameMode, difficultyName);
                gameOverDialog.dismiss();
                if (listener != null) {
                    listener.onRematch();
                }
            });
        }

        // Change Settings button
        View btnChangeSettings = dialogView.findViewById(R.id.gameOverChangeSettings);
        if (btnChangeSettings != null) {
            btnChangeSettings.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayEffect(GameAudio.EFFECT_MENU_TAP);
                }
                gameOverDialog.dismiss();
                if (winningPlayer == 1 && !passAndPlayMode && !tutorialMode) {
                    showReviewIfEligible(() -> {
                        if (listener != null) {
                            listener.onChangeSettings(winningPlayer, p1Name, p2Name, gameMode);
                        }
                    });
                } else {
                    if (listener != null) {
                        listener.onChangeSettings(winningPlayer, p1Name, p2Name, gameMode);
                    }
                }
            });
        }

        // Main Menu button
        View btnMainMenu = dialogView.findViewById(R.id.gameOverMainMenu);
        if (btnMainMenu != null) {
            btnMainMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayEffect(GameAudio.EFFECT_MENU_TAP);
                }
                gameOverDialog.dismiss();
                if (winningPlayer == 1 && !passAndPlayMode && !tutorialMode) {
                    showReviewIfEligible(() -> {
                        if (listener != null) {
                            listener.onMainMenu(winningPlayer, p1Name, p2Name, gameMode);
                        }
                    });
                } else {
                    if (listener != null) {
                        listener.onMainMenu(winningPlayer, p1Name, p2Name, gameMode);
                    }
                }
            });
        }

        gameOverDialog.show();
    }

    private void showInterstitialIfAllowed() {
        AdManager adMgr = MenuActivity.getSharedAdManager();
        if (adMgr != null) {
            adMgr.showInterstitialIfReady(activity, null);
        }
    }

    private void showReviewIfEligible(Runnable afterAction) {
        ReviewPromptManager reviewManager = new ReviewPromptManager(activity);
        reviewManager.onGameCompleted();

        if (!reviewManager.shouldShowPrompt()) {
            afterAction.run();
            return;
        }

        reviewManager.onPromptShown();

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_review, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog reviewDialog = builder.create();

        if (reviewDialog.getWindow() != null) {
            reviewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            reviewDialog.getWindow().setDimAmount(0.75f);
        }

        View btnYes = dialogView.findViewById(R.id.reviewYes);
        if (btnYes != null) {
            btnYes.setOnClickListener(v -> {
                reviewManager.onUserAccepted();
                reviewDialog.dismiss();
                ReviewPromptManager.openStorePage(activity);
                afterAction.run();
            });
        }

        View btnLater = dialogView.findViewById(R.id.reviewLater);
        if (btnLater != null) {
            btnLater.setOnClickListener(v -> {
                reviewManager.onPromptDismissed();
                reviewDialog.dismiss();
                afterAction.run();
            });
        }

        View btnNever = dialogView.findViewById(R.id.reviewNever);
        if (btnNever != null) {
            btnNever.setOnClickListener(v -> {
                reviewManager.onPromptDismissed();
                reviewManager.onPromptDismissed();
                reviewManager.onPromptDismissed();
                reviewDialog.dismiss();
                afterAction.run();
            });
        }

        reviewDialog.show();
    }

    public void dismissDialog() {
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dismiss();
        }
    }

    public boolean isDialogShowing() {
        return gameOverDialog != null && gameOverDialog.isShowing();
    }
}
