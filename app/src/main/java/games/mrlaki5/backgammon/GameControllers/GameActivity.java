package games.mrlaki5.backgammon.GameControllers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import games.mrlaki5.backgammon.Analytics.GameAnalytics;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.Economy.CoinConfig;
import games.mrlaki5.backgammon.Economy.CoinManager;
import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GameModel.ModelLoader;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.GameView.OnBoardImage;
import games.mrlaki5.backgammon.GameView.themes.BoardTheme;
import games.mrlaki5.backgammon.GameView.themes.BoardThemeFactory;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Menus.SettingsActivity;
import games.mrlaki5.backgammon.Monetization.ads.AdCallback;
import games.mrlaki5.backgammon.Monetization.ads.AdManager;
import games.mrlaki5.backgammon.Monetization.ads.RewardedAdPlacement;
import games.mrlaki5.backgammon.Monetization.ads.RewardedAdTracker;
import games.mrlaki5.backgammon.Players.BotMoveStrategy;
import games.mrlaki5.backgammon.Players.Human;
import games.mrlaki5.backgammon.Players.Player;
import games.mrlaki5.backgammon.R;
import games.mrlaki5.backgammon.Retention.AchievementManager;
import games.mrlaki5.backgammon.Retention.WeeklyChallenge;

/**
 * Main gameplay activity coordinating game loop, user interactions, audio,
 * and specialized controllers (ShakeController, TutorialController, GameOverHandler,
 * PauseMenuHandler, PassAndPlayManager).
 */
public class GameActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    // Audio & Settings
    private int soundVolume = 0;
    private boolean soundEnabled = true;
    private boolean effectsEnabled = true;
    private GameAudio gameAudio;
    private int timeBetweenTurns = 0;

    // Game Core Objects
    private GameLogic gameLogic;
    private GameMoveExecutor moveExecutor;
    private ModelLoader modelLoader;
    private Model model;
    private GameTask gameTask;
    private OnBoardImage BoardImage;

    // UI Controls
    private View rollDiceButton;
    private Button btnHint;
    private Button btnUndo;

    // Specialized Controllers
    private ShakeController shakeController;
    private TutorialController tutorialController;
    private GameOverHandler gameOverHandler;
    private PauseMenuHandler pauseMenuHandler;
    private PassAndPlayManager passAndPlayManager;

    // Economy & Ads
    private CoinManager coinManager;
    private RewardedAdTracker rewardedAdTracker;

    // Undo move snapshot
    private static class TurnSnapshot {
        final games.mrlaki5.backgammon.Beans.BoardFieldState[] boardFields;
        final games.mrlaki5.backgammon.Beans.DiceThrow[] diceThrows;
        final java.util.List<NextJump> nextMoves;

        TurnSnapshot(games.mrlaki5.backgammon.Beans.BoardFieldState[] fields,
                     games.mrlaki5.backgammon.Beans.DiceThrow[] dice,
                     java.util.List<NextJump> moves) {
            this.boardFields = new games.mrlaki5.backgammon.Beans.BoardFieldState[fields.length];
            for (int i = 0; i < fields.length; i++) {
                this.boardFields[i] = new games.mrlaki5.backgammon.Beans.BoardFieldState(
                        fields[i].getNumberOfChips(), fields[i].getPlayer());
            }
            this.diceThrows = new games.mrlaki5.backgammon.Beans.DiceThrow[dice.length];
            for (int i = 0; i < dice.length; i++) {
                this.diceThrows[i] = new games.mrlaki5.backgammon.Beans.DiceThrow(
                        dice[i].getThrowNumber(), dice[i].getAlreadyUsed());
            }
            this.nextMoves = new java.util.ArrayList<>();
            if (moves != null) {
                for (NextJump j : moves) {
                    this.nextMoves.add(new NextJump(j.getJumpNumber(), j.getSrcField(), j.getDstField()));
                }
            }
        }

        games.mrlaki5.backgammon.Beans.BoardFieldState[] copyBoard() {
            games.mrlaki5.backgammon.Beans.BoardFieldState[] copy =
                    new games.mrlaki5.backgammon.Beans.BoardFieldState[boardFields.length];
            for (int i = 0; i < boardFields.length; i++) {
                copy[i] = new games.mrlaki5.backgammon.Beans.BoardFieldState(
                        boardFields[i].getNumberOfChips(), boardFields[i].getPlayer());
            }
            return copy;
        }

        games.mrlaki5.backgammon.Beans.DiceThrow[] copyDice() {
            games.mrlaki5.backgammon.Beans.DiceThrow[] copy =
                    new games.mrlaki5.backgammon.Beans.DiceThrow[diceThrows.length];
            for (int i = 0; i < diceThrows.length; i++) {
                copy[i] = new games.mrlaki5.backgammon.Beans.DiceThrow(
                        diceThrows[i].getThrowNumber(), diceThrows[i].getAlreadyUsed());
            }
            return copy;
        }

        java.util.List<NextJump> copyMoves() {
            java.util.List<NextJump> copy = new java.util.ArrayList<>();
            for (NextJump j : nextMoves) {
                copy.add(new NextJump(j.getJumpNumber(), j.getSrcField(), j.getDstField()));
            }
            return copy;
        }
    }

    private TurnSnapshot turnSnapshot = null;
    private boolean undoUsedThisTurn = false;

    // Game Flow State
    private int pauseDone = 0;
    private int MoveFieldSrc;
    private int CurrentFingerPointer = -1;
    private long gameStartTimeMs;
    private int sessionGameNumber = 1;
    private boolean isRematchGame = false;

    private static final String EXTRA_SESSION_GAME_NUMBER = "session_game_number";
    private static final String EXTRA_IS_REMATCH = "is_rematch";

    // Touch listener for human checker moves
    private final View.OnTouchListener BoardListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x_touch = event.getX();
            float y_touch = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);

                    int touchedNum = BoardImage.triangleTouched(x_touch, y_touch);
                    boolean isTouched = BoardImage.chipPTouched(touchedNum, x_touch, y_touch);

                    if (isTouched && model != null) {
                        if (model.getBoardFields()[touchedNum].getPlayer() == model.getCurrentPlayer()) {
                            int[] currNextMoves = gameLogic.calculateNextMovesForSpecificField(
                                    model.getNextMoves(), touchedNum);
                            if (currNextMoves != null) {
                                model.getBoardFields()[touchedNum].setNumberOfChips(
                                        model.getBoardFields()[touchedNum].getNumberOfChips() - 1);
                                if (model.getBoardFields()[touchedNum].getNumberOfChips() == 0) {
                                    model.getBoardFields()[touchedNum].setPlayer(0);
                                }
                                BoardImage.setNextMoveArray(currNextMoves);
                                MoveFieldSrc = touchedNum;
                                BoardImage.setMoveChip(x_touch, y_touch, model.getCurrentPlayer());
                                BoardImage.postInvalidateOnAnimation();
                                CurrentFingerPointer = event.getPointerId(0);
                            } else {
                                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                            }
                        } else {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        if (event.getPointerId(i) == CurrentFingerPointer) {
                            x_touch = event.getX(i);
                            y_touch = event.getY(i);
                            break;
                        }
                    }
                    if (BoardImage.moveMoveChip(x_touch, y_touch)) {
                        BoardImage.postInvalidateOnAnimation();
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (CurrentFingerPointer == -1) {
                        break;
                    }
                    int tempFlag = 1;
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        if (event.getPointerId(i) == CurrentFingerPointer && event.getActionIndex() != i) {
                            tempFlag = 0;
                            break;
                        }
                    }
                    if (tempFlag == 0) {
                        break;
                    } else {
                        CurrentFingerPointer = -1;
                    }

                    x_touch = BoardImage.getXMovPos();
                    y_touch = BoardImage.getYMovPos();

                    if (BoardImage.unsetMoveChip()) {
                        int dstField = BoardImage.triangleTouched(x_touch, y_touch);
                        boolean moveApplied = false;

                        if (dstField != -1) {
                            GameMoveExecutor.MoveResult moveResult =
                                    moveExecutor.applyPickedUpMoveWithResult(MoveFieldSrc, dstField, model.getNextMoves());
                            moveApplied = moveResult.isApplied();
                            if (moveApplied) {
                                BoardImage.playMoveFeedback(moveResult.getDestinationField(), moveResult.isHit());
                                playMoveEffect(moveResult);

                                if (tutorialController.isTutorialMode()
                                        && tutorialController.getTutorialStep() >= TutorialScenarios.STEP_MOVE) {
                                    tutorialController.advanceToNextStep();
                                }
                            }
                        } else {
                            moveExecutor.applyPickedUpMove(MoveFieldSrc, -1, model.getNextMoves());
                        }

                        if (moveApplied) {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                            BoardImage.setDices(model.getDiceThrows());
                            model.setNextMoves(gameLogic.calculateMoves(model.getBoardFields(),
                                    model.getCurrentPlayer(), model.getDiceThrows()));
                        } else {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }

                        BoardImage.setNextMoveArray(null);

                        if (model.getNextMoves().isEmpty()) {
                            synchronized (model.getCurrentObjectPlayer()) {
                                model.getCurrentObjectPlayer().setWaitCond(0);
                                model.getCurrentObjectPlayer().notifyAll();
                            }
                        }
                        BoardImage.postInvalidateOnAnimation();
                    }
                    break;

                default:
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);

        applySelectedBoardTheme();
        gameAudio = new GameAudio(this);

        Bundle extras = getIntent().getExtras();
        boolean tutorialMode = extras != null && extras.getBoolean(MenuActivity.EXTRA_TUTORIAL_MODE, false);
        boolean passAndPlayMode = false;
        if (extras != null) {
            String gameMode = extras.getString(MenuActivity.EXTRA_GAME_MODE, "");
            passAndPlayMode = MenuActivity.GAME_MODE_PASS_AND_PLAY.equals(gameMode);
            sessionGameNumber = extras.getInt(EXTRA_SESSION_GAME_NUMBER, 1);
            isRematchGame = extras.getBoolean(EXTRA_IS_REMATCH, false);
        }

        // Initialize Settings
        SharedPreferences preferences = getSharedPreferences("Settings", 0);
        int shakeThreshold = preferences.getInt(SettingsActivity.KEY_DICE_TRESHOLD, SettingsActivity.DEF_DICE_TRAESHOLD);
        int sampleTime = preferences.getInt(SettingsActivity.KEY_TIME_SAMPLE, SettingsActivity.DEF_TIME_SAMPLE);
        int diceDelay = preferences.getInt(SettingsActivity.KEY_DICE_SHAKE_DELAY, SettingsActivity.DEF_DICE_SHAKE_DELAY);
        timeBetweenTurns = preferences.getInt(SettingsActivity.KEY_TIME_BETWEEN_TURNS, SettingsActivity.DEF_TIME_BETWEEN_TURNS);

        soundVolume = GamePreferences.getSfxVolume(this);
        soundEnabled = preferences.getBoolean(SettingsActivity.KEY_SOUND_ENABLED, SettingsActivity.DEF_SOUND_ENABLED);
        effectsEnabled = preferences.getBoolean(SettingsActivity.KEY_EFFECTS_ENABLED, SettingsActivity.DEF_EFFECTS_ENABLED);

        if (gameAudio != null) {
            gameAudio.startBackgroundMusic();
        }

        coinManager = new CoinManager(this);
        rewardedAdTracker = new RewardedAdTracker(this);

        // View Setup
        BoardImage = findViewById(R.id.boardImage);
        BoardImage.setBoardTheme(GamePreferences.getBoardTheme(this));

        rollDiceButton = findViewById(R.id.rollDiceButton);
        rollDiceButton.setOnClickListener(v -> rollDiceFromButton(v));
        setRollDiceButtonVisible(false);

        setupHintButton();

        // Model & Logic
        modelLoader = new ModelLoader();
        model = modelLoader.loadModel(extras, this);
        gameLogic = new GameLogic(model);
        moveExecutor = new GameMoveExecutor(model);

        // Subsystem Controllers
        initControllers(tutorialMode, passAndPlayMode, shakeThreshold, sampleTime, diceDelay);

        // Board initial draw
        BoardImage.setChipMatrix(model.getBoardFields());
        BoardImage.setDices(model.getDiceThrows());
        BoardImage.invalidate();

        // Start Game Thread
        gameTask = new GameTask(model, gameLogic, BoardImage, getTurnTransitionDelayMs(), this);
        gameTask.execute();

        trackGameStartedEvent();
    }

    private void initControllers(boolean tutorialMode, boolean passAndPlayMode,
                                int shakeThreshold, int sampleTime, int diceDelay) {
        // 1. ShakeController
        shakeController = new ShakeController(this);
        shakeController.updateSettings(shakeThreshold, sampleTime, diceDelay);
        shakeController.setOnShakeListener(new ShakeController.OnShakeListener() {
            @Override
            public void onShakeStarted() {
                setMPlayer(1);
            }

            @Override
            public void onShakeCompleted() {
                completeHumanDiceRoll();
            }
        });

        // 2. TutorialController
        LinearLayout tutorialPanel = findViewById(R.id.tutorialPanel);
        TextView tutorialBody = findViewById(R.id.tutorialBody);
        Button tutorialNextButton = findViewById(R.id.tutorialNextButton);
        tutorialController = new TutorialController(this, tutorialMode,
                tutorialPanel, tutorialBody, tutorialNextButton);
        tutorialController.setGameDependencies(model, gameLogic, BoardImage);
        tutorialController.setListener(new TutorialController.OnTutorialListener() {
            @Override
            public void onPlayEffect(int effectId) {
                playEffect(effectId);
            }

            @Override
            public void onTutorialStepCompleted(int step) {
                trackTutorialStepCompleted(step);
            }
        });

        // 3. PassAndPlayManager
        FrameLayout turnSwitchOverlay = findViewById(R.id.turnSwitchRoot);
        TextView turnSwitchMessage = findViewById(R.id.turnSwitchMessage);
        Button turnSwitchReady = findViewById(R.id.turnSwitchReady);
        passAndPlayManager = new PassAndPlayManager(this, passAndPlayMode,
                turnSwitchOverlay, turnSwitchMessage, turnSwitchReady);
        passAndPlayManager.setListener(effectId -> playEffect(effectId));

        // 4. PauseMenuHandler
        pauseMenuHandler = new PauseMenuHandler(this);
        pauseMenuHandler.setListener(new PauseMenuHandler.OnPauseMenuActionListener() {
            @Override
            public void onPlayEffect(int effectId) {
                playEffect(effectId);
            }

            @Override
            public void onResumeGame() {}

            @Override
            public void onRestartGame() {
                restartGame();
            }

            @Override
            public void onQuitGame() {
                String mode = isTutorialMode() ? "tutorial" : (isPassAndPlayMode() ? "pass_and_play" : "vs_bot");
                GameAnalytics.get().trackGameAbandoned(mode, getDifficultyName(), getGameDurationSeconds());
                if (!isTutorialMode() && !isPassAndPlayMode()) {
                    new AchievementManager(GameActivity.this).onGameAbandoned();
                }
                leaveMethod();
                Intent data = new Intent();
                setResult(MenuActivity.GAME_PRESSED_BACK, data);
                finish();
            }

            @Override
            public void onVolumeChanged(int newVolume) {
                soundVolume = newVolume;
            }
        });

        findViewById(R.id.btnPause).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            pauseMenuHandler.showPauseDialog();
        });

        // 5. GameOverHandler
        gameOverHandler = new GameOverHandler(this, coinManager);
        gameOverHandler.setListener(new GameOverHandler.OnGameOverActionListener() {
            @Override
            public void onPlayEffect(int effectId) {
                playEffect(effectId);
            }

            @Override
            public void onRematch() {
                startRematch();
            }

            @Override
            public void onChangeSettings(int winningPlayer, String p1Name, String p2Name, String gameMode) {
                finishWithResult(winningPlayer, p1Name, p2Name, gameMode);
            }

            @Override
            public void onMainMenu(int winningPlayer, String p1Name, String p2Name, String gameMode) {
                finishWithResult(winningPlayer, p1Name, p2Name, gameMode);
            }
        });
    }

    public void activateTouchListener() {
        if (turnSnapshot == null && !undoUsedThisTurn && model != null) {
            turnSnapshot = new TurnSnapshot(model.getBoardFields(), model.getDiceThrows(), model.getNextMoves());
        }
        BoardImage.setOnTouchListener(BoardListener);
        if (tutorialController.isTutorialMode()) {
            tutorialController.showTutorialText(tutorialController.getTutorialStep());
        }
    }

    public void deactivateTouchListener() {
        BoardImage.setOnTouchListener(null);
    }

    public void activateShakeListener() {
        turnSnapshot = null;
        undoUsedThisTurn = false;
        waitForTutorialIntro();
        if (tutorialController.isTutorialMode() && tutorialController.getTutorialStep() == TutorialScenarios.STEP_INTRO) {
            tutorialController.setTutorialStep(TutorialScenarios.STEP_ROLL);
            tutorialController.loadTutorialScenario(TutorialScenarios.STEP_ROLL);
        } else if (tutorialController.isTutorialMode()) {
            tutorialController.showTutorialText(tutorialController.getTutorialStep());
        }
        shakeController.startListening();
        setRollDiceButtonVisible(true);
    }

    public void deactivateShakeListener() {
        shakeController.stopListening();
        setRollDiceButtonVisible(false);
    }

    private void setRollDiceButtonVisible(final boolean visible) {
        if (rollDiceButton == null) return;
        runOnUiThread(() -> {
            rollDiceButton.animate().cancel();
            rollDiceButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            rollDiceButton.setEnabled(visible);
            if (visible) {
                rollDiceButton.setScaleX(0.88F);
                rollDiceButton.setScaleY(0.88F);
                rollDiceButton.setAlpha(0F);
                rollDiceButton.animate()
                        .alpha(1F)
                        .scaleX(1F)
                        .scaleY(1F)
                        .setDuration(170L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        });
    }

    private void completeHumanDiceRoll() {
        if (model == null || gameLogic == null || BoardImage == null) return;

        Player currentPlayer = model.getCurrentObjectPlayer();
        if (currentPlayer == null || !(currentPlayer instanceof Human)) return;

        synchronized (currentPlayer) {
            if (currentPlayer.getWaitCond() != 1) return;

            shakeController.setShakeStarted(2);
            deactivateShakeListener();
            setMPlayer(2);
            model.setDiceThrows(gameLogic.rollDices());
            BoardImage.setDices(model.getDiceThrows());
            BoardImage.invalidate();
            currentPlayer.setWaitCond(0);
            currentPlayer.notifyAll();

            if (tutorialController.isTutorialMode()
                    && tutorialController.getTutorialStep() == TutorialScenarios.STEP_ROLL) {
                tutorialController.advanceToNextStep();
            }
        }
    }

    public void rollDiceFromButton(View view) {
        playEffect(GameAudio.EFFECT_MENU_TAP);
        if (model == null) return;

        int state = model.getState();
        if (state == 0 || state == 1 || state == 3) {
            completeHumanDiceRoll();
        }
    }

    public void waitForTutorialIntro() {
        if (tutorialController != null) {
            tutorialController.waitForTutorialIntro();
        }
    }

    public void advanceToNextTutorialStep() {
        if (tutorialController != null) {
            tutorialController.advanceToNextStep();
        }
    }

    public void showTutorialMessage(final int step) {
        if (tutorialController != null) {
            tutorialController.showTutorialMessage(step);
        }
    }

    public void advanceTutorial(View view) {
        // XML onClick compat — no-op
    }

    public void showTurnSwitchAndWait(String nextPlayerName, int nextPlayer) {
        if (passAndPlayManager != null) {
            passAndPlayManager.showTurnSwitchAndWait(nextPlayerName, nextPlayer, BoardImage);
        }
    }

    public boolean isPassAndPlayMode() {
        return passAndPlayManager != null && passAndPlayManager.isPassAndPlayMode();
    }

    public boolean isTutorialMode() {
        return tutorialController != null && tutorialController.isTutorialMode();
    }

    @Override
    public void onBackPressed() {
        if (gameOverHandler != null && gameOverHandler.isDialogShowing()) {
            gameOverHandler.dismissDialog();
            finish();
            return;
        }
        if (pauseMenuHandler != null) {
            pauseMenuHandler.showPauseDialog();
        }
    }

    private void restartGame() {
        String mode = isTutorialMode() ? "tutorial" : (isPassAndPlayMode() ? "pass_and_play" : "vs_bot");
        GameAnalytics.get().trackGameRestarted(mode, getDifficultyName());
        leaveMethod();
        File file = new File(getFilesDir().getAbsolutePath(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        file.delete();
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (pauseDone == 1) {
            File file = new File(getFilesDir().getAbsolutePath(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
            file.delete();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pauseDone == 1) {
            pauseDone = 0;
            if (gameAudio != null) {
                gameAudio.startBackgroundMusic();
            }
            gameTask = new GameTask(model, gameLogic, BoardImage, getTurnTransitionDelayMs(), this);
            gameTask.execute();
        }
    }

    private int getTurnTransitionDelayMs() {
        return Math.max(80, Math.min(550, 80 + (timeBetweenTurns * 90)));
    }

    private void applySelectedBoardTheme() {
        int themeId = GamePreferences.getBoardTheme(this);
        BoardTheme theme = BoardThemeFactory.getTheme(themeId);
        findViewById(R.id.gameRoot).setBackgroundResource(theme.getBackgroundDrawableRes());
    }

    @Override
    protected void onPause() {
        if (gameTask != null) {
            gameTask.setWorkFlag(0);
            synchronized (gameTask) {
                if (gameTask.getEndRoutineStarted() == 0) {
                    gameTask.setEndRoutineStarted(1);
                    pauseDone = 1;
                }
            }
            if (pauseDone == 1) {
                synchronized (model.getCurrentObjectPlayer()) {
                    model.getCurrentObjectPlayer().setWaitCond(0);
                    model.getCurrentObjectPlayer().notifyAll();
                }
                clearMPlayer();
                if (gameAudio != null) {
                    gameAudio.stopBackgroundMusic();
                }
                shakeController.stopListening();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameAudio != null) {
            gameAudio.release();
            gameAudio = null;
        }
        if (gameTask != null) {
            gameTask.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (pauseDone == 1) {
            synchronized (gameTask) {
                while (gameTask.getFinishedFlag() != 1) {
                    try {
                        gameTask.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            modelLoader.saveModel(model, this);
        }
        super.onStop();
    }

    public void leaveMethod() {
        if (passAndPlayManager != null) {
            passAndPlayManager.release();
        }
        if (tutorialController != null) {
            tutorialController.release();
        }
        if (gameTask != null) {
            gameTask.setWorkFlag(0);
            synchronized (model.getCurrentObjectPlayer()) {
                model.getCurrentObjectPlayer().setWaitCond(0);
                model.getCurrentObjectPlayer().notifyAll();
            }
            clearMPlayer();
            shakeController.stopListening();
            int tempFlag = 0;
            synchronized (gameTask) {
                if (gameTask.getEndRoutineStarted() == 0) {
                    gameTask.setEndRoutineStarted(1);
                    tempFlag = 1;
                }
                while (gameTask.getFinishedFlag() != 1) {
                    try {
                        gameTask.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (tempFlag == 1) {
                modelLoader.saveModel(model, this);
            }
            if (gameOverHandler != null && !gameOverHandler.isResultRecorded()
                    && !isPassAndPlayMode() && !isTutorialMode()) {
                WeeklyChallenge weeklyChallenge = new WeeklyChallenge(this);
                weeklyChallenge.onGameAbandoned();
            }
        }
    }

    public void clearMPlayer() {
        if (gameAudio != null) {
            gameAudio.stopDiceShake();
        }
    }

    public void onCheckerMoved(final GameMoveExecutor.MoveResult moveResult) {
        if (moveResult == null || !moveResult.isApplied() || BoardImage == null) {
            return;
        }
        runOnUiThread(() -> {
            BoardImage.playMoveFeedback(moveResult.getDestinationField(), moveResult.isHit());
            playMoveEffect(moveResult);
        });
    }

    public void playGameFinishedEffect() {
        runOnUiThread(() -> playEffect(GameAudio.EFFECT_GAME_WIN));
    }

    private void playEffect(int effectNum) {
        if (!soundEnabled || !effectsEnabled || soundVolume <= 0) {
            return;
        }
        if (gameAudio != null) {
            gameAudio.play(effectNum);
        }
    }

    private void playMoveEffect(GameMoveExecutor.MoveResult moveResult) {
        if (moveResult.getDestinationField() == 26 || moveResult.getDestinationField() == 27) {
            playEffect(GameAudio.EFFECT_BEAR_OFF);
        } else {
            playEffect(moveResult.isHit()
                    ? GameAudio.EFFECT_CHECKER_HIT
                    : GameAudio.EFFECT_CHECKER_MOVE);
        }
    }

    public void setMPlayer(int SongNum) {
        if (!soundEnabled || GamePreferences.getSfxVolume(this) <= 0) {
            clearMPlayer();
            return;
        }
        if (gameAudio == null) return;
        if (SongNum == 1) {
            gameAudio.startDiceShake();
        } else {
            gameAudio.finishDiceRoll();
        }
    }

    // Getters and Setters
    public GameLogic getGameLogic() {
        return gameLogic;
    }

    public void setGameLogic(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    public OnBoardImage getBoardImage() {
        return BoardImage;
    }

    public void setBoardImage(OnBoardImage boardImage) {
        BoardImage = boardImage;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    // Analytics helpers
    private void trackGameStartedEvent() {
        gameStartTimeMs = System.currentTimeMillis();
        String mode;
        if (isTutorialMode()) {
            mode = "tutorial";
            GameAnalytics.get().trackTutorialStarted();
        } else if (isPassAndPlayMode()) {
            mode = "pass_and_play";
        } else {
            mode = "vs_bot";
        }
        GameAnalytics.get().trackGameStarted(mode, getDifficultyName(), getThemeName());
        GameAnalytics.get().setUserProperty("last_game_number", String.valueOf(sessionGameNumber));
    }

    public void trackTutorialStepCompleted(int step) {
        GameAnalytics.get().trackTutorialStep(step);
        if (step >= TutorialScenarios.TOTAL_STEPS - 1) {
            GameAnalytics.get().trackTutorialCompleted();
            new AchievementManager(this).onTutorialCompleted();
        }
    }

    public long getGameDurationSeconds() {
        return (System.currentTimeMillis() - gameStartTimeMs) / 1000;
    }

    private String getDifficultyName() {
        int diff = GamePreferences.getBotDifficulty(this);
        switch (diff) {
            case 0: return "easy";
            case 1: return "medium";
            case 2: return "hard";
            case 3: return "royal";
            default: return "unknown";
        }
    }

    private String getThemeName() {
        int theme = GamePreferences.getBoardTheme(this);
        switch (theme) {
            case GamePreferences.THEME_POP_ART: return "pop_art";
            case GamePreferences.THEME_CYBERPUNK: return "cyberpunk";
            case GamePreferences.THEME_LUXURY: return "luxury";
            default: return "royal";
        }
    }

    public void onGameFinished(int winningPlayer, String p1Name, String p2Name, String gameMode) {
        if (gameOverHandler != null) {
            gameOverHandler.handleGameFinished(winningPlayer, p1Name, p2Name, gameMode,
                    isPassAndPlayMode(), isTutorialMode(), isRematchGame,
                    sessionGameNumber, getGameDurationSeconds(), getDifficultyName());
        }
    }

    private void finishWithResult(int winningPlayer, String p1Name, String p2Name, String gameMode) {
        Intent data = new Intent();
        data.putExtra(MenuActivity.EXTRA_PLAYER1_NAME, p1Name);
        data.putExtra(MenuActivity.EXTRA_PLAYER2_NAME, p2Name);
        data.putExtra(MenuActivity.EXTRA_WINING_PLAYER, winningPlayer);
        data.putExtra(MenuActivity.EXTRA_GAME_MODE, gameMode);
        setResult(MenuActivity.GAME_ENDED_OK, data);
        finish();
    }

    private void startRematch() {
        sessionGameNumber++;
        String mode = isPassAndPlayMode() ? "pass_and_play" : "vs_bot";
        GameAnalytics.get().trackRematchStarted(mode, getDifficultyName());

        File saveFile = new File(getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        saveFile.delete();

        Intent intent = getIntent();
        intent.putExtra(EXTRA_SESSION_GAME_NUMBER, sessionGameNumber);
        intent.putExtra(EXTRA_IS_REMATCH, true);
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupHintButton() {
        btnHint = findViewById(R.id.btnHint);
        if (btnHint != null) {
            btnHint.setOnClickListener(v -> showHintChoiceDialog());
        }
        btnUndo = findViewById(R.id.btnUndo);
        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> performUndo());
        }
    }

    private void performUndo() {
        if (model == null || model.getState() != 2 || turnSnapshot == null || undoUsedThisTurn) {
            android.widget.Toast.makeText(this, R.string.undo_unavailable, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if (coinManager != null && coinManager.spend(CoinConfig.UNDO_COST, "undo_move")) {
            undoUsedThisTurn = true;
            model.setBoardFields(turnSnapshot.copyBoard());
            model.setDiceThrows(turnSnapshot.copyDice());
            model.setNextMoves(turnSnapshot.copyMoves());
            BoardImage.setChipMatrix(model.getBoardFields());
            BoardImage.setDices(model.getDiceThrows());
            BoardImage.setNextMoveArray(null);
            BoardImage.postInvalidateOnAnimation();
            android.widget.Toast.makeText(this, R.string.undo_success, android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, R.string.insufficient_coins, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void showHintChoiceDialog() {
        if (model == null || model.getState() != 2 || model.getNextMoves() == null || model.getNextMoves().isEmpty()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.hint_choice_title);
        String[] options = new String[]{
                getString(R.string.hint_use_coins, CoinConfig.HINT_COST),
                getString(R.string.hint_watch_video)
        };
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (coinManager != null && coinManager.spend(CoinConfig.HINT_COST, "hint")) {
                    revealBestMove();
                } else {
                    android.widget.Toast.makeText(this, R.string.insufficient_coins, android.widget.Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) {
                if (rewardedAdTracker != null && rewardedAdTracker.canShow(RewardedAdPlacement.HINT)) {
                    AdManager adMgr = MenuActivity.getSharedAdManager();
                    if (adMgr != null && adMgr.isRewardedAdReady()) {
                        adMgr.showRewardedAd(this, RewardedAdPlacement.HINT, new AdCallback() {
                            @Override
                            public void onAdLoaded() {}

                            @Override
                            public void onAdFailedToLoad(String error) {
                                runOnUiThread(() -> android.widget.Toast.makeText(GameActivity.this,
                                        R.string.iap_purchase_failed, android.widget.Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onAdShown() {}

                            @Override
                            public void onAdDismissed() {}

                            @Override
                            public void onAdClicked() {}

                            @Override
                            public void onRewardEarned() {
                                if (rewardedAdTracker != null) {
                                    rewardedAdTracker.recordShow(RewardedAdPlacement.HINT);
                                }
                                GameAnalytics.get().trackRewardedAdWatched("hint");
                                runOnUiThread(() -> revealBestMove());
                            }
                        });
                    } else {
                        if (adMgr != null) adMgr.preloadAds();
                        android.widget.Toast.makeText(this, R.string.iap_purchase_failed, android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.widget.Toast.makeText(this, R.string.insufficient_coins, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    private void revealBestMove() {
        if (model == null || model.getNextMoves() == null || model.getNextMoves().isEmpty() || BoardImage == null || gameLogic == null) return;
        BotMoveStrategy strategy = new BotMoveStrategy();
        NextJump bestMove = strategy.chooseMove(model, model.getNextMoves(), 3, new java.util.Random());
        if (bestMove != null) {
            int[] currNextMoves = gameLogic.calculateNextMovesForSpecificField(model.getNextMoves(), bestMove.getSrcField());
            if (currNextMoves != null) {
                BoardImage.setNextMoveArray(currNextMoves);
                BoardImage.invalidate();
                android.widget.Toast.makeText(this, R.string.hint_best_move, android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
}
