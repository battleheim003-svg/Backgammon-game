package games.mrlaki5.backgammon.GameControllers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import games.mrlaki5.backgammon.Database.DbHelper;
import games.mrlaki5.backgammon.Database.GameResultRecorder;
import games.mrlaki5.backgammon.Database.ScoresTableEntry;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Analytics.GameAnalytics;
import games.mrlaki5.backgammon.WinStreakTracker;
import games.mrlaki5.backgammon.Retention.AchievementManager;
import games.mrlaki5.backgammon.Retention.DailyChallenge;
import games.mrlaki5.backgammon.Retention.ReviewPromptManager;
import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GameModel.ModelLoader;
import games.mrlaki5.backgammon.GameView.OnBoardImage;
import games.mrlaki5.backgammon.Players.Human;
import games.mrlaki5.backgammon.Players.Player;
import games.mrlaki5.backgammon.R;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.Menus.SettingsActivity;

//Game activity
public class GameActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    //Current sound volume in which player should play
    private int soundVolume=0;
    private boolean soundEnabled=true;
    private boolean effectsEnabled=true;
    private GameAudio gameAudio;
    //Object for some game related rules
    private GameLogic gameLogic;
    //Object for applying legal checker moves to the model
    private GameMoveExecutor moveExecutor;
    //View
    private OnBoardImage BoardImage;
    //On-screen alternative to phone shaking.
    private View rollDiceButton;
    private LinearLayout tutorialPanel;
    private TextView tutorialBody;
    private Button tutorialNextButton;
    private boolean tutorialMode=false;
    private volatile boolean tutorialIntroBlocking = false;
    private int tutorialStep=0;
    private int[] tutorialMessages;
    //Object for loading model
    private ModelLoader modelLoader;
    //Model (used for storing game state)
    private Model model;
    //Thread in which game turns are run
    private GameTask gameTask;
    //Flag used in onPause and in onStop to know cause of game stoping
    private int pauseDone=0;
    //Time between player turns
    private int timeBetweenTurns=0;
    //Used in touch listener to save starting field of moving chip
    private int MoveFieldSrc;
    //Used in touch listener to save id of chip moving finger
    private int CurrentFingerPointer=-1;
    //sensor manager for shake listener
    private SensorManager sensorManager;
    //sensor for shake listener
    private Sensor sensor;
    //Time of last update in shake listener
    private long lastUpdate=0;
    //x acceleration value in last update in shake listener
    private float last_x=0;
    //y acceleration value in last update in shake listener
    private float last_y=0;
    //z acceleration value in last update in shake listener
    private float last_z=0;
    //Treshold of shake listener
    private int shake_treshold = 100;
    //Time between two updates of shake listener
    private int sample_time;
    //Delay in starting and ending shake
    private int dice_delay;
    //Flag for determination if shake started
    private int shakeStarted=0;
    //Stability counter used for shake start delay
    private int beforeShakeStability=0;
    //Stability counter used for shake end delay
    private int shakeStability=0;

    // --- Pass & Play mode fields ---
    private boolean passAndPlayMode = false;
    private FrameLayout turnSwitchOverlay;
    private TextView turnSwitchMessage;
    private Button turnSwitchReady;
    private final Object turnSwitchLock = new Object();
    private volatile boolean turnSwitchWaiting = false;

    //Touch listener activated when human needs to move chips
    private View.OnTouchListener BoardListener= new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //Get current touch x and y coordinates
            float x_touch= event.getX();
            float y_touch= event.getY();
            //which touch action is done
            switch(event.getActionMasked()) {
                //First finger is put on screen
                case MotionEvent.ACTION_DOWN:
                    //Keep the board in control of the gesture and give immediate feedback.
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    //Find a number of currently touched triangle
                    int touchedNum=BoardImage.triangleTouched(x_touch,y_touch);
                    //Find if chips on that triangle are touched
                    boolean isTouched=BoardImage.chipPTouched(touchedNum, x_touch, y_touch);
                    if(isTouched){
                        //Check if touched chips are from current player
                        if(model.getBoardFields()[touchedNum].getPlayer()
                                ==model.getCurrentPlayer()) {
                            int[] currNextMoves;
                            //Calculate moves for currently touched chip
                            currNextMoves = gameLogic.calculateNextMovesForSpecificField(
                                    model.getNextMoves(), touchedNum);
                            if (currNextMoves !=null) {
                                //Lower number of chips from touched triangle
                                model.getBoardFields()[touchedNum].setNumberOfChips(
                                        model.getBoardFields()[touchedNum].getNumberOfChips()-1);
                                //If there are no left chips, remove player from triangle
                                if(model.getBoardFields()[touchedNum].getNumberOfChips()==0) {
                                    model.getBoardFields()[touchedNum].setPlayer(0);
                                }
                                //Update view with hints where can picked chip move
                                BoardImage.setNextMoveArray(currNextMoves);
                                //Set source field of moving field
                                MoveFieldSrc=touchedNum;
                                //Update view, set moving chip
                                BoardImage.setMoveChip(x_touch, y_touch, model.getCurrentPlayer());
                                //Invalidate view to draw again
                                BoardImage.postInvalidateOnAnimation();
                                //Save current finger id
                                CurrentFingerPointer=event.getPointerId(0);
                            }
                            else {
                                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                            }
                        }
                        else {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        }
                    }
                    break;
                //Some finger moved
                case MotionEvent.ACTION_MOVE:
                    //Check if moving finger is chip finger
                    for(int i=0; i<event.getPointerCount(); i++){
                        if(event.getPointerId(i)==CurrentFingerPointer){
                            x_touch=event.getX(i);
                            y_touch=event.getY(i);
                            break;
                        }
                    }
                    //Move chip to finger position, update view
                    if(BoardImage.moveMoveChip(x_touch, y_touch)) {
                        BoardImage.postInvalidateOnAnimation();
                    }
                    break;
                //Some finger left screen
                case MotionEvent.ACTION_POINTER_UP:
                    //Last finger left screen
                case MotionEvent.ACTION_UP:
                    //Check if chip finger didnt left already (==-1)
                    if(CurrentFingerPointer==-1){
                        break;
                    }
                    //Check if chip finger is leaving now
                    int tempFlag=1;
                    for(int i=0; i<event.getPointerCount(); i++){
                        if(event.getPointerId(i)==CurrentFingerPointer
                                && event.getActionIndex()!=i){
                            tempFlag=0;
                            break;
                        }
                    }
                    //If it is not break, if it is set it to -1
                    if(tempFlag==0){
                        break;
                    }
                    else{
                        CurrentFingerPointer=-1;
                    }
                    //Get x and y chip coordinates from view
                    x_touch=BoardImage.getXMovPos();
                    y_touch=BoardImage.getYMovPos();
                    //Unset move chip
                    if(BoardImage.unsetMoveChip()) {
                        //Calculate destination triangle
                        int dstField = BoardImage.triangleTouched(x_touch,y_touch);
                        boolean moveApplied = false;
                        if(dstField!=-1) {
                            GameMoveExecutor.MoveResult moveResult =
                                    moveExecutor.applyPickedUpMoveWithResult(MoveFieldSrc,
                                            dstField, model.getNextMoves());
                            moveApplied = moveResult.isApplied();
                            if(moveApplied){
                                BoardImage.playMoveFeedback(moveResult.getDestinationField(),
                                        moveResult.isHit());
                                playMoveEffect(moveResult);
                                // Tutorial: move complete → load next scenario
                                if (tutorialMode && tutorialStep >= TutorialScenarios.STEP_MOVE) {
                                    advanceToNextTutorialStep();
                                }
                            }
                        }
                        else {
                            moveExecutor.applyPickedUpMove(MoveFieldSrc, -1,
                                    model.getNextMoves());
                        }
                        if(moveApplied) {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                            BoardImage.setDices(model.getDiceThrows());
                            //Calculate next moves after chip move
                            model.setNextMoves(gameLogic.calculateMoves(model.getBoardFields(),
                                    model.getCurrentPlayer(), model.getDiceThrows()));
                        }
                        else {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }
                        //Update view, remove hints
                        BoardImage.setNextMoveArray(null);
                        //If there are no more next moves return to GameTask and continue turns
                        if(model.getNextMoves().isEmpty()){
                            synchronized (model.getCurrentObjectPlayer()){
                                model.getCurrentObjectPlayer().setWaitCond(0);
                                model.getCurrentObjectPlayer().notifyAll();
                            }
                        }
                        //Draw view again
                        BoardImage.postInvalidateOnAnimation();
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    //Shake listener activated when human needs to roll dices
    private SensorEventListener DiceListener= new SensorEventListener() {

        //Is called sensor reacts to change of values
        @Override
        public void onSensorChanged(SensorEvent event) {
            //Find type of sensor reacting
            switch(event.sensor.getType()){
                //If its accelerometer
                case Sensor.TYPE_ACCELEROMETER:
                    //Get current time
                    long curTime = System.currentTimeMillis();
                    //Only allow one update every sample_time
                    if ((curTime - lastUpdate) > sample_time) {
                        long diffTime = (curTime - lastUpdate);
                        //Update lastupdate time to current
                        lastUpdate = curTime;
                        //Get current x,y,z values
                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];
                        //Calculate speed
                        float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;
                        //If speed is greater then Trashold
                        if (speed > shake_treshold) {
                            //If Shake didnt started yet and before shake delay is done
                            if(shakeStarted==0 && beforeShakeStability>=dice_delay) {
                                //Start roll dice sound
                                setMPlayer(1);
                                //Set shakeStarted flag
                                shakeStarted=1;
                            }
                            else{
                                //If delay is not over decrease it
                                beforeShakeStability++;
                            }
                            //Set after shake stability to zero ass long as speed is more then
                            // trashold (reset delay)
                            shakeStability=0;
                        }
                        //If speed is lower then trashold
                        else{
                            //Decrease after shake stability
                            shakeStability++;
                            //While shake speed is not more then trashold set before shake stability
                            //to zero (reset delay)
                            beforeShakeStability=0;
                            //If end delay is over and shake started, end shake
                            if(shakeStability>=dice_delay && shakeStarted==1) {
                                completeHumanDiceRoll();
                            }
                        }
                        //Update last x, y, z values
                        last_x = x;
                        last_y = y;
                        last_z = z;
                    }
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //Blank
        }
    };

    //Method called when activating touch listener
    public void activateTouchListener(){
        BoardImage.setOnTouchListener(BoardListener);
        // In tutorial: show the current step's text (scenario already loaded)
        if (tutorialMode) {
            showTutorialText(tutorialStep);
        }
    }

    //Method called when deactivating touch listener
    public void deactivateTouchListener(){
        BoardImage.setOnTouchListener(null);
    }

    //Method called when activating shake listener
    public void activateShakeListener(){
        // Block until tutorial intro is dismissed
        waitForTutorialIntro();
        // In tutorial mode: if we just passed intro, load the ROLL scenario
        if (tutorialMode && tutorialStep == TutorialScenarios.STEP_INTRO) {
            tutorialStep = TutorialScenarios.STEP_ROLL;
            loadTutorialScenario(TutorialScenarios.STEP_ROLL);
        } else if (tutorialMode) {
            showTutorialText(tutorialStep);
        }
        //Reset all important values to starting
        shakeStarted=0;
        beforeShakeStability=0;
        lastUpdate=0;
        last_x=0;
        last_y=0;
        last_z=0;
        //Register listener
        sensorManager.registerListener(DiceListener, sensor, SensorManager.SENSOR_DELAY_GAME);
        setRollDiceButtonVisible(true);
    }

    //Method called when deactivating shake listener
    public void deactivateShakeListener(){
        sensorManager.unregisterListener(DiceListener);
        setRollDiceButtonVisible(false);
    }

    private void setRollDiceButtonVisible(final boolean visible){
        if(rollDiceButton==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rollDiceButton.animate().cancel();
                rollDiceButton.setVisibility(visible ? View.VISIBLE : View.GONE);
                rollDiceButton.setEnabled(visible);
                if(visible){
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
            }
        });
    }

    //Completes a human dice roll from either a phone shake or the on-screen button.
    private void completeHumanDiceRoll(){
        if(model==null || gameLogic==null || BoardImage==null){
            return;
        }

        Player currentPlayer=model.getCurrentObjectPlayer();
        if(currentPlayer==null || !(currentPlayer instanceof Human)){
            return;
        }

        synchronized (currentPlayer){
            //Human.actionRoll() sets waitCond to 1 while it is waiting for a valid roll.
            //This also prevents double taps and keeps the button inactive during bot turns.
            if(currentPlayer.getWaitCond()!=1){
                return;
            }

            shakeStarted=2;
            deactivateShakeListener();
            setMPlayer(2);
            model.setDiceThrows(gameLogic.rollDices());
            BoardImage.setDices(model.getDiceThrows());
            BoardImage.invalidate();
            currentPlayer.setWaitCond(0);
            currentPlayer.notifyAll();
            // Tutorial: dice rolled → advance to next scenario
            if (tutorialMode && tutorialStep == TutorialScenarios.STEP_ROLL) {
                advanceToNextTutorialStep();
            }
        }
    }

    //Called by the "roll dice" button in activity_game.xml.
    public void rollDiceFromButton(View view){
        playEffect(GameAudio.EFFECT_MENU_TAP);
        if(model==null){
            return;
        }

        int state=model.getState();
        if(state==0 || state==1 || state==3){
            completeHumanDiceRoll();
        }
    }

    //Method called on creation of GameActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Part for removing status bar from screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);
        applySelectedBoardTheme();
        rollDiceButton=findViewById(R.id.rollDiceButton);
        rollDiceButton.setOnClickListener(v -> rollDiceFromButton(v));
        setRollDiceButtonVisible(false);
        // Pause button
        findViewById(R.id.btnPause).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            showPauseDialog();
        });
        gameAudio = new GameAudio(this);
        //Get sent extras from menu activity (they dont exist if game is continued,
        // only if its new game)
        Bundle extras=getIntent().getExtras();
        tutorialMode=extras!=null && extras.getBoolean(MenuActivity.EXTRA_TUTORIAL_MODE, false);
        // Check if game mode is Pass & Play
        if (extras != null) {
            String gameMode = extras.getString(MenuActivity.EXTRA_GAME_MODE, "");
            passAndPlayMode = MenuActivity.GAME_MODE_PASS_AND_PLAY.equals(gameMode);
        }
        //Get values of shared preferences (game settings parameters)
        SharedPreferences preferences = getSharedPreferences("Settings", 0);
        //Get shake treshold value
        shake_treshold=preferences.getInt(SettingsActivity.KEY_DICE_TRESHOLD,
                SettingsActivity.DEF_DICE_TRAESHOLD);
        //Get time value between two shake sensor events
        sample_time=preferences.getInt(SettingsActivity.KEY_TIME_SAMPLE,
                SettingsActivity.DEF_TIME_SAMPLE);
        //Get value of shake delays
        dice_delay=preferences.getInt(SettingsActivity.KEY_DICE_SHAKE_DELAY,
                SettingsActivity.DEF_DICE_SHAKE_DELAY);
        //Get value of time between turns in game
        timeBetweenTurns=preferences.getInt(SettingsActivity.KEY_TIME_BETWEEN_TURNS,
                SettingsActivity.DEF_TIME_BETWEEN_TURNS);
        //Get value of sound volume
        soundVolume=GamePreferences.getSfxVolume(this);
        soundEnabled=preferences.getBoolean(SettingsActivity.KEY_SOUND_ENABLED,
                SettingsActivity.DEF_SOUND_ENABLED);
        effectsEnabled=preferences.getBoolean(SettingsActivity.KEY_EFFECTS_ENABLED,
                SettingsActivity.DEF_EFFECTS_ENABLED);
        if(gameAudio!=null){
            gameAudio.startBackgroundMusic();
        }
        //Get View
        BoardImage=((OnBoardImage)findViewById(R.id.boardImage) );
        BoardImage.setBoardTheme(GamePreferences.getBoardTheme(this));
        //Create model loader
        modelLoader=new ModelLoader();
        //Build model
        model=modelLoader.loadModel(extras, this);
        //Create game logics
        gameLogic = new GameLogic(model);
        //Create model move executor
        moveExecutor = new GameMoveExecutor(model);
        //Create game task
        gameTask=new GameTask(model, gameLogic, BoardImage,
                getTurnTransitionDelayMs(), this);
        //Update view with chip matrix from model
        BoardImage.setChipMatrix(model.getBoardFields());
        //Update view with dice throws from model
        BoardImage.setDices(model.getDiceThrows());
        //Invalidate view, it is drawn
        BoardImage.invalidate();
        setupTutorialPanel();
        setupTurnSwitchOverlay();
        //Get sensor manager
        sensorManager=(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        //Get sensor
        sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Start game thread
        gameTask.execute();

        // Track game started analytics
        trackGameStartedEvent();

        // Initialize session game counter (incremented on rematch)
        Bundle gameExtras = getIntent().getExtras();
        if (gameExtras != null) {
            sessionGameNumber = gameExtras.getInt(EXTRA_SESSION_GAME_NUMBER, 1);
            isRematchGame = gameExtras.getBoolean(EXTRA_IS_REMATCH, false);
        } else {
            sessionGameNumber = 1;
            isRematchGame = false;
        }
        gameResultRecorded = false;
    }

    private void setupTutorialPanel() {
        tutorialPanel = findViewById(R.id.tutorialPanel);
        tutorialBody = findViewById(R.id.tutorialBody);
        tutorialNextButton = findViewById(R.id.tutorialNextButton);
        tutorialMessages = new int[]{
                R.string.tutorial_intro,    // 0
                R.string.tutorial_roll,     // 1
                R.string.tutorial_move,     // 2
                R.string.tutorial_hit,      // 3
                R.string.tutorial_bar,      // 4
                R.string.tutorial_bear_off, // 5
                R.string.tutorial_finished  // 6
        };
        if (tutorialMode) {
            tutorialIntroBlocking = true;
            tutorialStep = 0;
            tutorialPanel.setVisibility(View.VISIBLE);
            tutorialBody.setText(tutorialMessages[0]);
            tutorialNextButton.setVisibility(View.VISIBLE);
            tutorialNextButton.setOnClickListener(v -> {
                playEffect(GameAudio.EFFECT_MENU_TAP);
                tutorialNextButton.setVisibility(View.GONE);
                tutorialIntroBlocking = false;
                synchronized (tutorialPanel) {
                    tutorialPanel.notifyAll();
                }
            });
        } else {
            tutorialPanel.setVisibility(View.GONE);
        }
    }

    /**
     * Blocks until the user taps "Next" on the intro screen.
     */
    public void waitForTutorialIntro() {
        if (!tutorialIntroBlocking) return;
        synchronized (tutorialPanel) {
            while (tutorialIntroBlocking) {
                try {
                    tutorialPanel.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Loads the next tutorial scenario. Called when the player completes an action.
     * Sets up the board for the next lesson and shows the instruction.
     */
    public void advanceToNextTutorialStep() {
        if (!tutorialMode) return;
        tutorialStep++;
        if (tutorialStep >= TutorialScenarios.TOTAL_STEPS) {
            // Tutorial complete
            runOnUiThread(() -> {
                tutorialPanel.setVisibility(View.GONE);
            });
            return;
        }
        loadTutorialScenario(tutorialStep);
    }

    /**
     * Loads a specific tutorial scenario onto the board.
     */
    private void loadTutorialScenario(int step) {
        if (model == null || step >= TutorialScenarios.TOTAL_STEPS) return;

        if (step == TutorialScenarios.STEP_DONE) {
            // Final step — just show message
            showTutorialText(step);
            return;
        }

        // Apply scenario to model
        TutorialScenarios.applyScenario(model, step);

        // Update the board view
        if (BoardImage != null) {
            BoardImage.setChipMatrix(model.getBoardFields());
            BoardImage.setDices(model.getDiceThrows());
            BoardImage.postInvalidate();
        }

        // Recalculate legal moves
        if (gameLogic != null && model.getState() == 2) {
            model.setNextMoves(gameLogic.calculateMoves(
                    model.getBoardFields(), model.getCurrentPlayer(), model.getDiceThrows()));
        }

        // Show tutorial text
        showTutorialText(step);
    }

    private void showTutorialText(int step) {
        final int safeStep = Math.min(step, tutorialMessages.length - 1);
        runOnUiThread(() -> {
            tutorialPanel.setVisibility(View.VISIBLE);
            tutorialBody.setText(tutorialMessages[safeStep]);

            // Bear-off step: move panel to top so it doesn't cover the last checker
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                            tutorialPanel.getLayoutParams();
            if (safeStep == TutorialScenarios.STEP_BEAR_OFF) {
                params.bottomToBottom = -1;
                params.topToTop = R.id.boardImage;
                params.topMargin = (int) (14 * getResources().getDisplayMetrics().density);
            } else {
                params.topToTop = -1;
                params.bottomToBottom = R.id.boardImage;
                params.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);
            }
            tutorialPanel.setLayoutParams(params);

            if (safeStep >= tutorialMessages.length - 1) {
                tutorialPanel.postDelayed(() -> {
                    if (tutorialPanel != null) tutorialPanel.setVisibility(View.GONE);
                }, 4000);
            }
        });
    }

    /**
     * Called by game events. Shows contextual tutorial text without loading scenarios.
     * Used for steps triggered by game flow (roll prompt, etc).
     */
    public void showTutorialMessage(final int step) {
        if (!tutorialMode || tutorialPanel == null || tutorialMessages == null) return;
        showTutorialText(step);
    }

    public void advanceTutorial(View view) {
        // XML onClick compat — no-op
    }

    private void setupTurnSwitchOverlay() {
        turnSwitchOverlay = findViewById(R.id.turnSwitchRoot);
        turnSwitchMessage = findViewById(R.id.turnSwitchMessage);
        turnSwitchReady = findViewById(R.id.turnSwitchReady);
        if (turnSwitchReady != null) {
            turnSwitchReady.setOnClickListener(v -> {
                playEffect(GameAudio.EFFECT_MENU_TAP);
                hideTurnSwitchOverlay();
            });
        }
    }

    /**
     * Shows the turn-switch overlay. Called from GameTask (background thread).
     * Blocks until the user taps "I'm Ready".
     */
    public void showTurnSwitchAndWait(String nextPlayerName, int nextPlayer) {
        if (!passAndPlayMode || turnSwitchOverlay == null) return;

        turnSwitchWaiting = true;
        runOnUiThread(() -> {
            String msg = getString(R.string.turn_switch_message, nextPlayerName);
            turnSwitchMessage.setText(msg);
            turnSwitchOverlay.setVisibility(View.VISIBLE);
            turnSwitchOverlay.setAlpha(0f);
            turnSwitchOverlay.animate().alpha(1f).setDuration(200).start();
            // Rotate board for player 2 (so they see from their perspective)
            if (BoardImage != null) {
                float rotation = (nextPlayer == 2) ? 180f : 0f;
                BoardImage.animate().rotation(rotation).setDuration(300).start();
            }
        });

        // Block the game thread until ready is tapped
        synchronized (turnSwitchLock) {
            while (turnSwitchWaiting) {
                try {
                    turnSwitchLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void hideTurnSwitchOverlay() {
        if (turnSwitchOverlay == null) return;
        turnSwitchOverlay.animate().alpha(0f).setDuration(150).withEndAction(() ->
                turnSwitchOverlay.setVisibility(View.GONE)
        ).start();
        synchronized (turnSwitchLock) {
            turnSwitchWaiting = false;
            turnSwitchLock.notifyAll();
        }
    }

    /**
     * Returns true if the game is in Pass & Play mode.
     */
    public boolean isPassAndPlayMode() {
        return passAndPlayMode;
    }

    /**
     * Returns true if the game is in tutorial mode.
     */
    public boolean isTutorialMode() {
        return tutorialMode;
    }

    //Method called when back button is pressed
    @Override
    public void onBackPressed() {
        // If game-over dialog is showing, back = main menu
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dismiss();
            finish();
            return;
        }
        showPauseDialog();
    }

    // --- Pause Dialog ---

    private AlertDialog pauseDialog;

    private void showPauseDialog() {
        if (pauseDialog != null && pauseDialog.isShowing()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pause, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        pauseDialog = builder.create();

        // Transparent background so our custom drawable shows
        if (pauseDialog.getWindow() != null) {
            pauseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            pauseDialog.getWindow().setDimAmount(0.7f);
        }

        // Resume button
        dialogView.findViewById(R.id.pauseResume).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            pauseDialog.dismiss();
        });

        // Restart button
        dialogView.findViewById(R.id.pauseRestart).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            pauseDialog.dismiss();
            restartGame();
        });

        // Quit button
        dialogView.findViewById(R.id.pauseQuit).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            pauseDialog.dismiss();
            // Track game abandoned
            String mode = tutorialMode ? "tutorial" : (passAndPlayMode ? "pass_and_play" : "vs_bot");
            GameAnalytics.get().trackGameAbandoned(mode, getDifficultyName(), getGameDurationSeconds());
            // Update achievements (reset no-quit counter)
            if (!tutorialMode && !passAndPlayMode) {
                new AchievementManager(GameActivity.this).onGameAbandoned();
            }
            leaveMethod();
            Intent data = new Intent();
            setResult(MenuActivity.GAME_PRESSED_BACK, data);
            finish();
        });

        // Volume SeekBar
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.pauseVolumeSeekBar);
        volumeSeekBar.setProgress(GamePreferences.getSfxVolume(this));
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    soundVolume = progress;
                    GamePreferences.saveAudioVolumes(GameActivity.this, progress, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        pauseDialog.show();
    }

    private void restartGame() {
        // Track game restarted
        String mode = tutorialMode ? "tutorial" : (passAndPlayMode ? "pass_and_play" : "vs_bot");
        GameAnalytics.get().trackGameRestarted(mode, getDifficultyName());
        // Stop current game
        leaveMethod();
        // Delete save file
        java.io.File file = new java.io.File(getFilesDir().getAbsolutePath(),
                MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        file.delete();
        // Recreate activity with same intent (restarts the game)
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    //Method called after onCreate, before onResume. Must be called if on closing onStop was called
    @Override
    protected void onStart() {
        super.onStart();
        //If pause was done it means that game was closed with onStop, so model was loaded from file
        //and save file must be deleted
        if(pauseDone==1){
            File file=new File(GameActivity.this.getFilesDir().getAbsolutePath(),
                    MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
            file.delete();
        }
    }

    //Method called after onStart. Must be called if on closing onPause was called
    @Override
    protected void onResume() {
        super.onResume();
        //If onPause was called which shut down game thread, so new one must be created and started
        if(pauseDone==1){
            //Flag reset
            pauseDone=0;
            if(gameAudio!=null){
                gameAudio.startBackgroundMusic();
            }
            //New game thread created
            gameTask=new GameTask(model, gameLogic, BoardImage,
                    getTurnTransitionDelayMs(), this);
            //New game thread started
            gameTask.execute();
        }
    }

    private int getTurnTransitionDelayMs() {
        return Math.max(80, Math.min(550, 80 + (timeBetweenTurns * 90)));
    }

    private void applySelectedBoardTheme() {
        int drawable;
        switch (GamePreferences.getBoardTheme(this)) {
            case GamePreferences.THEME_POP_ART:
                drawable = R.drawable.board_pop_art;
                break;
            case GamePreferences.THEME_CYBERPUNK:
                drawable = R.drawable.board_cyberpunk;
                break;
            case GamePreferences.THEME_LUXURY:
                drawable = R.drawable.board_luxury;
                break;
            default:
                drawable = R.drawable.board_royal;
        }
        findViewById(R.id.gameRoot).setBackgroundResource(drawable);
    }

    //Method called on pausing activity
    @Override
    protected void onPause() {
        if(gameTask!=null) {
            //Set work flag in game thread to 0
            gameTask.setWorkFlag(0);
            //Synchronize end of activity with setting endRoutineStarted to 1
            //which will stop oll other endActivity attempts
            synchronized (gameTask) {
                if (gameTask.getEndRoutineStarted() == 0) {
                    gameTask.setEndRoutineStarted(1);
                    pauseDone=1;
                }
            }
            //If current pause ending activity is first one
            if(pauseDone==1){
                //Shut down game thread if it was waiting
                synchronized (model.getCurrentObjectPlayer()) {
                    model.getCurrentObjectPlayer().setWaitCond(0);
                    model.getCurrentObjectPlayer().notifyAll();
                }
                //Stop sound player
                clearMPlayer();
                if(gameAudio!=null){
                    gameAudio.stopBackgroundMusic();
                }
                //Unregister shake listener if it was registered
                sensorManager.unregisterListener(DiceListener);
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(gameAudio!=null){
            gameAudio.release();
            gameAudio=null;
        }
        super.onDestroy();
    }

    //Method called on stop activity, after onPause activity
    @Override
    protected void onStop() {
        //If current pause ending activity is first one
        if(pauseDone==1){
            synchronized (gameTask){
                //Wait until game thread is finished
                while (gameTask.getFinishedFlag() != 1) {
                    try {
                        gameTask.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            //Save model to file
            modelLoader.saveModel(model, this);
        }
        super.onStop();
    }

    //Method used on back pressed to shut down all resources
    public void leaveMethod(){
        // Release turn switch overlay if waiting
        synchronized (turnSwitchLock) {
            turnSwitchWaiting = false;
            turnSwitchLock.notifyAll();
        }
        // Release tutorial intro block if waiting
        tutorialIntroBlocking = false;
        if (tutorialPanel != null) {
            synchronized (tutorialPanel) {
                tutorialPanel.notifyAll();
            }
        }
        if(gameTask!=null){
            //Set work flag in game thread to 0
            gameTask.setWorkFlag(0);
            //Shut down game thread if it was waiting
            synchronized (model.getCurrentObjectPlayer()) {
                model.getCurrentObjectPlayer().setWaitCond(0);
                model.getCurrentObjectPlayer().notifyAll();
            }
            //Stop sound player
            clearMPlayer();
            //Unregister shake listener if it was registered
            sensorManager.unregisterListener(DiceListener);
            int tempFlag=0;
            //Synchronize end of activity with setting endRoutineStarted to 1
            //which will stop oll other endActivity attempts
            synchronized (gameTask) {
                if(gameTask.getEndRoutineStarted()==0){
                    gameTask.setEndRoutineStarted(1);
                    tempFlag=1;
                }
                //Wait until game thread is finished
                while (gameTask.getFinishedFlag() != 1) {
                    try {
                        gameTask.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            //if back pressed is first end of activity save model to file
            if(tempFlag==1) {
                modelLoader.saveModel(model, this);
            }
        }
    }

    //Method for stopping and clearing sound player
    public void clearMPlayer(){
        if(gameAudio!=null){
            gameAudio.stopDiceShake();
        }
    }

    public void onCheckerMoved(final GameMoveExecutor.MoveResult moveResult) {
        if(moveResult==null || !moveResult.isApplied() || BoardImage==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BoardImage.playMoveFeedback(moveResult.getDestinationField(), moveResult.isHit());
                playMoveEffect(moveResult);
            }
        });
    }

    public void playGameFinishedEffect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playEffect(GameAudio.EFFECT_GAME_WIN);
            }
        });
    }

    private float normalizedVolume() {
        if(!soundEnabled || soundVolume<=0){
            return 0F;
        }
        return Math.max(0F, Math.min(1F, soundVolume / 100F));
    }

    private void playEffect(int effectNum) {
        if(!soundEnabled || !effectsEnabled || soundVolume<=0){
            return;
        }
        if(gameAudio!=null){
            gameAudio.play(effectNum);
        }
    }

    private void playMoveEffect(GameMoveExecutor.MoveResult moveResult) {
        if(moveResult.getDestinationField()==26 || moveResult.getDestinationField()==27){
            playEffect(GameAudio.EFFECT_BEAR_OFF);
        }
        else {
            playEffect(moveResult.isHit()
                    ? GameAudio.EFFECT_CHECKER_HIT
                    : GameAudio.EFFECT_CHECKER_MOVE);
        }
    }

    //Method for playing sounds on media player
    //SongNum= 1:diceShake, 2:diceRoll
    public void setMPlayer(int SongNum){
        if(!soundEnabled || GamePreferences.getSfxVolume(this)<=0){
            clearMPlayer();
            return;
        }
        if(gameAudio==null){
            return;
        }
        if(SongNum==1){
            gameAudio.startDiceShake();
        }
        else{
            gameAudio.finishDiceRoll();
        }
    }

    //Getters and setters
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

    // ==================== Analytics helpers ====================

    private long gameStartTimeMs;
    private int sessionGameNumber = 0; // Increments each game in this session (including rematches)
    private boolean gameResultRecorded = false; // Prevents duplicate stat recording
    private boolean isRematchGame = false; // True if this game was started via Rematch button

    private void trackGameStartedEvent() {
        gameStartTimeMs = System.currentTimeMillis();
        String mode;
        if (tutorialMode) {
            mode = "tutorial";
            GameAnalytics.get().trackTutorialStarted();
        } else if (passAndPlayMode) {
            mode = "pass_and_play";
        } else {
            mode = "vs_bot";
        }
        String difficulty = getDifficultyName();
        String theme = getThemeName();
        GameAnalytics.get().trackGameStarted(mode, difficulty, theme);
        // Set user property for session depth analysis
        GameAnalytics.get().setUserProperty("last_game_number",
                String.valueOf(sessionGameNumber));
    }

    /**
     * Called by GameTask when a tutorial step completes.
     */
    public void trackTutorialStepCompleted(int step) {
        GameAnalytics.get().trackTutorialStep(step);
        if (step >= TutorialScenarios.TOTAL_STEPS - 1) {
            GameAnalytics.get().trackTutorialCompleted();
            new AchievementManager(this).onTutorialCompleted();
        }
    }

    /**
     * Returns the duration of the current game in seconds.
     */
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

    // ==================== Game Over / Rematch ====================

    /**
     * Called by GameTask when a player wins. Shows the game-over dialog
     * with Rematch / Change Settings / Main Menu options.
     *
     * Also handles: statistics recording, ELO update, analytics, and ad display.
     */
    public void onGameFinished(int winningPlayer, String p1Name, String p2Name, String gameMode) {
        // Record statistics (only once per game)
        if (!gameResultRecorded) {
            gameResultRecorded = true;
            recordGameResult(winningPlayer, p1Name, p2Name, gameMode);

            // Increment ad frequency counter (once per completed game)
            games.mrlaki5.backgammon.Monetization.ads.AdManager adMgr =
                    MenuActivity.getSharedAdManager();
            if (adMgr != null) {
                adMgr.onGameCompleted();
            }
        }

        // Track analytics
        long duration = getGameDurationSeconds();
        String winner = (winningPlayer == 1) ? "player" : "opponent";
        if (passAndPlayMode) {
            winner = (winningPlayer == 1) ? "player1" : "player2";
        }
        GameAnalytics.get().trackGameCompleted(gameMode, winner, duration,
                sessionGameNumber, 0, 0);
        if (!passAndPlayMode && !tutorialMode) {
            if (winningPlayer == 1) {
                GameAnalytics.get().trackGameWon(gameMode, getDifficultyName(), duration);
            } else {
                GameAnalytics.get().trackGameLost(gameMode, getDifficultyName(), duration);
            }
        }
        // Track rematch_completed if this game was started as a rematch
        if (isRematchGame) {
            GameAnalytics.get().trackRematchCompleted(gameMode, getDifficultyName(), winner);
        }

        // Delete save file (game is complete, no need to continue)
        File saveFile = new File(getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        saveFile.delete();

        // Update win streak (only for vs_bot, not tutorial/pass_and_play)
        int currentStreak = 0;
        if (!passAndPlayMode && !tutorialMode) {
            WinStreakTracker streakTracker = new WinStreakTracker(this);
            if (winningPlayer == 1) {
                currentStreak = streakTracker.recordWin();
            } else {
                streakTracker.recordLoss();
            }

            // Update achievements
            AchievementManager achievements = new AchievementManager(this);
            int difficulty = GamePreferences.getBotDifficulty(this);
            achievements.onGameCompleted(winningPlayer == 1, difficulty, currentStreak);

            // Update daily challenge
            DailyChallenge dailyChallenge = new DailyChallenge(this);
            dailyChallenge.onGameCompleted(winningPlayer == 1, difficulty, false, currentStreak);
        }

        // Show game-over dialog on UI thread
        final String winnerName = (winningPlayer == 1) ? p1Name : p2Name;
        final int streak = currentStreak;
        runOnUiThread(() -> {
            // Show interstitial ad if frequency cap allows (once per game end)
            showInterstitialIfAllowed();
            showGameOverDialog(winnerName, winningPlayer, p1Name, p2Name, gameMode, streak);
        });
    }

    private void recordGameResult(int winningPlayer, String p1Name, String p2Name, String gameMode) {
        // Legacy database
        try {
            DbHelper helper = new DbHelper(this);
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
            GameResultRecorder recorder = new GameResultRecorder(this);
            String winnerName = (winningPlayer == 1) ? p1Name : p2Name;
            String loserName = (winningPlayer == 1) ? p2Name : p1Name;
            recorder.recordResult(winnerName, loserName, gameMode);
        } catch (Exception e) {
            GameAnalytics.get().reportError(e, "recordGameResult_elo");
        }
    }

    private AlertDialog gameOverDialog;

    private void showGameOverDialog(String winnerName, int winningPlayer,
                                    String p1Name, String p2Name, String gameMode, int winStreak) {
        if (isFinishing() || isDestroyed()) return;
        if (gameOverDialog != null && gameOverDialog.isShowing()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_over, null);

        // Set winner text
        TextView winnerText = dialogView.findViewById(R.id.gameOverWinner);
        winnerText.setText(getString(R.string.game_over_winner, winnerName));

        // Set game duration
        TextView durationText = dialogView.findViewById(R.id.gameOverDuration);
        long durationSec = getGameDurationSeconds();
        int minutes = (int) (durationSec / 60);
        int seconds = (int) (durationSec % 60);
        String durationStr = getString(R.string.game_over_duration, minutes, seconds);
        if (sessionGameNumber > 1) {
            durationStr += "  •  " + getString(R.string.game_over_game_number, sessionGameNumber);
        }
        durationText.setText(durationStr);

        // Show win streak (only in vs_bot, only after a win, streak >= 2)
        TextView streakText = dialogView.findViewById(R.id.gameOverStreak);
        if (!passAndPlayMode && !tutorialMode && winningPlayer == 1 && winStreak >= 2) {
            streakText.setVisibility(View.VISIBLE);
            streakText.setText(getString(R.string.game_over_win_streak, winStreak));
        } else if (!passAndPlayMode && !tutorialMode) {
            // Show daily challenge completion if just completed
            DailyChallenge dc = new DailyChallenge(this);
            if (dc.isCompleted()) {
                streakText.setVisibility(View.VISIBLE);
                streakText.setText(getString(R.string.daily_challenge_done));
            } else {
                streakText.setVisibility(View.GONE);
            }
        } else {
            streakText.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        gameOverDialog = builder.create();

        if (gameOverDialog.getWindow() != null) {
            gameOverDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            gameOverDialog.getWindow().setDimAmount(0.75f);
        }

        // --- Rematch button (primary) ---
        dialogView.findViewById(R.id.gameOverRematch).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            GameAnalytics.get().trackRematchClicked(gameMode, getDifficultyName());
            gameOverDialog.dismiss();
            startRematch();
        });

        // --- Change Settings button ---
        dialogView.findViewById(R.id.gameOverChangeSettings).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            gameOverDialog.dismiss();
            // Maybe show review prompt (only after a win)
            if (winningPlayer == 1 && !passAndPlayMode && !tutorialMode) {
                showReviewIfEligible(() -> finishWithResult(winningPlayer, p1Name, p2Name, gameMode));
            } else {
                finishWithResult(winningPlayer, p1Name, p2Name, gameMode);
            }
        });

        // --- Main Menu button ---
        dialogView.findViewById(R.id.gameOverMainMenu).setOnClickListener(v -> {
            playEffect(GameAudio.EFFECT_MENU_TAP);
            gameOverDialog.dismiss();
            // Maybe show review prompt (only after a win)
            if (winningPlayer == 1 && !passAndPlayMode && !tutorialMode) {
                showReviewIfEligible(() -> finishWithResult(winningPlayer, p1Name, p2Name, gameMode));
            } else {
                finishWithResult(winningPlayer, p1Name, p2Name, gameMode);
            }
        });

        gameOverDialog.show();
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

    private void showInterstitialIfAllowed() {
        games.mrlaki5.backgammon.Monetization.ads.AdManager adMgr =
                MenuActivity.getSharedAdManager();
        if (adMgr != null) {
            adMgr.showInterstitialIfReady(this, null);
        }
    }

    /**
     * Shows a review prompt dialog if conditions are met, then runs the action.
     * If conditions aren't met, runs the action immediately.
     */
    private void showReviewIfEligible(Runnable afterAction) {
        ReviewPromptManager reviewManager = new ReviewPromptManager(this);
        reviewManager.onGameCompleted(); // Always count the game

        if (!reviewManager.shouldShowPrompt()) {
            afterAction.run();
            return;
        }

        // Show review dialog
        reviewManager.onPromptShown();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_review, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog reviewDialog = builder.create();

        if (reviewDialog.getWindow() != null) {
            reviewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            reviewDialog.getWindow().setDimAmount(0.75f);
        }

        dialogView.findViewById(R.id.reviewYes).setOnClickListener(v -> {
            reviewManager.onUserAccepted();
            reviewDialog.dismiss();
            ReviewPromptManager.openStorePage(this);
            afterAction.run();
        });

        dialogView.findViewById(R.id.reviewLater).setOnClickListener(v -> {
            reviewManager.onPromptDismissed();
            reviewDialog.dismiss();
            afterAction.run();
        });

        dialogView.findViewById(R.id.reviewNever).setOnClickListener(v -> {
            // Dismiss 3 times = never show again
            reviewManager.onPromptDismissed();
            reviewManager.onPromptDismissed();
            reviewManager.onPromptDismissed();
            reviewDialog.dismiss();
            afterAction.run();
        });

        reviewDialog.show();
    }

    /**
     * Starts a rematch: recreates the activity with the same intent extras.
     * This reuses the existing game initialization logic in onCreate/ModelLoader.
     */
    private void startRematch() {
        sessionGameNumber++;
        // Track rematch started
        String mode = passAndPlayMode ? "pass_and_play" : "vs_bot";
        GameAnalytics.get().trackRematchStarted(mode, getDifficultyName());

        // Delete any stale save file
        File saveFile = new File(getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        saveFile.delete();

        // Recreate activity with same intent (starts fresh game with same settings)
        Intent intent = getIntent();
        // Pass session game number and rematch flag for tracking
        intent.putExtra(EXTRA_SESSION_GAME_NUMBER, sessionGameNumber);
        intent.putExtra(EXTRA_IS_REMATCH, true);
        finish();
        startActivity(intent);
        // Skip default activity transition for smoother feel
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** Extra key for passing game number across rematches within a session. */
    private static final String EXTRA_SESSION_GAME_NUMBER = "session_game_number";
    /** Extra key to mark a game as a rematch (for analytics). */
    private static final String EXTRA_IS_REMATCH = "is_rematch";
}
