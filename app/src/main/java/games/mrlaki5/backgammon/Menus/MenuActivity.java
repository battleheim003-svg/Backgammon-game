package games.mrlaki5.backgammon.Menus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.FrameLayout;
import android.widget.Spinner;

import java.io.File;

import games.mrlaki5.backgammon.Analytics.GameAnalytics;
import games.mrlaki5.backgammon.GameControllers.GameActivity;
import games.mrlaki5.backgammon.GameAudio;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.MenuAudioManager;
import games.mrlaki5.backgammon.R;

//Activity class for main menu
public class MenuActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    //Player1 name key intent value
    public static String EXTRA_PLAYER1_NAME="p1name";
    //Player2 name key intent value
    public static String EXTRA_PLAYER2_NAME="p2name";
    //Player1 kind key intent value
    public static String EXTRA_PLAYER1_KIND="p1kind";
    //Player2 kind key intent value
    public static String EXTRA_PLAYER2_KIND="p2kind";
    //Wining player key intent value
    public static String EXTRA_WINING_PLAYER="pWin";
    public static String EXTRA_TUTORIAL_MODE="tutorialMode";
    public static String EXTRA_GAME_MODE="gameMode";
    public static String GAME_MODE_PASS_AND_PLAY="pass_and_play";
    public static String GAME_MODE_VS_BOT="vs_bot";
    //Name of save file
    public static String GAME_CONTINUE_SAVE_FILE_NAME="gameSave";
    //Value of return int after game finishes for back pressed
    public static final int GAME_PRESSED_BACK=55;
    //Value of return int after game finishes for player won
    public static final int GAME_ENDED_OK=56;
    //Value of send int to starting a game
    public static final int REQUEST_CODE_GAME=65;
    //Dialog opened before new game starts
    private AlertDialog myDialog;
    //View of dialog opened before new game starts
    private View myView;
    private GameAudio gameAudio;
    private games.mrlaki5.backgammon.Monetization.ads.AdManager adManager;

    /**
     * Returns the shared AdManager instance.
     * Can be used by GameActivity for rewarded ads (hints).
     */
    public games.mrlaki5.backgammon.Monetization.ads.AdManager getAdManager() {
        return adManager;
    }

    // Static reference for GameActivity to access the ad manager
    private static games.mrlaki5.backgammon.Monetization.ads.AdManager sharedAdManager;

    public static games.mrlaki5.backgammon.Monetization.ads.AdManager getSharedAdManager() {
        return sharedAdManager;
    }

    //Listener used to catch cancel button click on new game dialog
    private View.OnClickListener CancelListener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playMenuTap();
            //Delete dialog
            if (myDialog != null){
                myDialog.dismiss();
                myDialog = null;
                myView=null;
            }
        }
    };

    //Listener used to catch play button click on new game dialog
    private View.OnClickListener PlayListener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playMenuTap();
            if (myDialog != null && myView!=null){
                //Load player1 name and player2 name from dialog
                String playerName1=((EditText)myView.findViewById(R.id.dialogPName1))
                        .getText().toString();
                String playerName2=((EditText)myView.findViewById(R.id.dialogPName2))
                        .getText().toString();
                //If player names are written
                if((!playerName1.isEmpty()) && (!playerName2.isEmpty())){
                    //Load id of checked radio button of player kind
                    int idRGp1=((RadioGroup)myView.findViewById(R.id.dialogRadioGP1))
                            .getCheckedRadioButtonId();
                    int idRGp2=((RadioGroup)myView.findViewById(R.id.dialogRadioGP2))
                            .getCheckedRadioButtonId();
                    //If player kinds are chosen
                    if(idRGp1!=-1 && idRGp2!=-1){
                        //Load player kinds from dialog view
                        String playerKind1 =
                                (idRGp1 == R.id.radioButton) ? "Player" : "Bot";
                        String playerKind2 =
                                (idRGp2 == R.id.radioButton3) ? "Player" : "Bot";
                        Spinner difficulty = myView.findViewById(R.id.dialogBotDifficulty);
                        Spinner theme = myView.findViewById(R.id.dialogBoardTheme);
                        GamePreferences.saveSelections(MenuActivity.this,
                                difficulty.getSelectedItemPosition(),
                                theme.getSelectedItemPosition());
                        //Delete dialog
                        myDialog.dismiss();
                        myDialog = null;
                        myView=null;
                        //If file with saved game exists, delete it
                        File file=new File(MenuActivity.this.getFilesDir().getAbsolutePath(),
                                MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
                        file.delete();
                        //Start game activity with new players
                        Intent intent=
                                new Intent(MenuActivity.this, GameActivity.class);
                        intent.putExtra(EXTRA_PLAYER1_NAME, playerName1);
                        intent.putExtra(EXTRA_PLAYER2_NAME, playerName2);
                        intent.putExtra(EXTRA_PLAYER1_KIND, playerKind1);
                        intent.putExtra(EXTRA_PLAYER2_KIND, playerKind2);
                        startActivityForResult(intent, REQUEST_CODE_GAME);
                    }
                }
            }
        }
    };

    //Method called on creation of MenuActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Part for removing status bar from screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_menu);
        gameAudio = new GameAudio(this);
        // Initialize ad manager with real TapsellAdProvider
        adManager = new games.mrlaki5.backgammon.Monetization.ads.AdManager(this,
                new games.mrlaki5.backgammon.Monetization.ads.TapsellAdProvider(
                        games.mrlaki5.backgammon.Monetization.ads.AdConfig.ZONE_INTERSTITIAL,
                        games.mrlaki5.backgammon.Monetization.ads.AdConfig.ZONE_REWARDED));
        adManager.initialize(this);
        adManager.preloadAds();
        sharedAdManager = adManager;
        //Load preferences
        SharedPreferences preferences = getSharedPreferences("Settings", 0);
        //If values in preferences dont exist (on first start), create them
        if(!preferences.contains(SettingsActivity.KEY_DICE_TRESHOLD)){
            SharedPreferences.Editor editor=preferences.edit();
            //Set shake treshold value
            editor.putInt(SettingsActivity.KEY_DICE_TRESHOLD,
                    SettingsActivity.DEF_DICE_TRAESHOLD);
            //Set time value between two shake sensor events
            editor.putInt(SettingsActivity.KEY_TIME_SAMPLE,
                    SettingsActivity.DEF_TIME_SAMPLE);
            //Set value of sound volume
            editor.putInt(SettingsActivity.KEY_SOUND_VOLUME,
                    SettingsActivity.DEF_SOUND_VOLUME);
            editor.putInt(GamePreferences.KEY_SFX_VOLUME,
                    GamePreferences.DEFAULT_SFX_VOLUME);
            editor.putInt(GamePreferences.KEY_MUSIC_VOLUME,
                    GamePreferences.DEFAULT_MUSIC_VOLUME);
            editor.putBoolean(SettingsActivity.KEY_SOUND_ENABLED,
                    SettingsActivity.DEF_SOUND_ENABLED);
            editor.putBoolean(SettingsActivity.KEY_EFFECTS_ENABLED,
                    SettingsActivity.DEF_EFFECTS_ENABLED);
            //Set value of shake delays
            editor.putInt(SettingsActivity.KEY_DICE_SHAKE_DELAY,
                    SettingsActivity.DEF_DICE_SHAKE_DELAY);
            //Set value of time between turns in game
            editor.putInt(SettingsActivity.KEY_TIME_BETWEEN_TURNS,
                    SettingsActivity.DEF_TIME_BETWEEN_TURNS);
            //Set default shake treshold value
            editor.putInt(SettingsActivity.KEY_DEF_DICE_TRESHOLD,
                    SettingsActivity.DEF_DICE_TRAESHOLD);
            //Set default time value between two shake sensor events
            editor.putInt(SettingsActivity.KEY_DEF_TIME_SAMPLE,
                    SettingsActivity.DEF_TIME_SAMPLE);
            //Set default value of sound volume
            editor.putInt(SettingsActivity.KEY_DEF_SOUND_VOLUME,
                    SettingsActivity.DEF_SOUND_VOLUME);
            //Set default value of shake delays
            editor.putInt(SettingsActivity.KEY_DEF_DICE_SHAKE_DELAY,
                    SettingsActivity.DEF_DICE_SHAKE_DELAY);
            //Set default value of time between turns in game
            editor.putInt(SettingsActivity.KEY_DEF_TIME_BETWEEN_TURNS,
                    SettingsActivity.DEF_TIME_BETWEEN_TURNS);
            editor.apply();
        }
        //Change color of continue game button from gray to yellow if save file exists
        checkAndChangeButtonColor();
        polishMenuButtons();
        // Start menu background music
        MenuAudioManager.get().startMenuMusic(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MenuAudioManager.get().startMenuMusic(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MenuAudioManager.get().pauseMenuMusic();
    }

    private void polishMenuButtons() {
        int[] buttonIds = {
                R.id.playGame,
                R.id.passAndPlay,
                R.id.tutorial,
                R.id.scores,
                R.id.settings,
                R.id.languageToggle
        };
        for (int i = 0; i < buttonIds.length; i++) {
            View button = findViewById(buttonIds[i]);
            if (button == null) {
                continue;
            }
            button.setOnTouchListener(menuButtonTouchListener);
            button.setAlpha(0F);
            button.setTranslationY(18F);
            button.animate()
                    .alpha(1F)
                    .translationY(0F)
                    .setStartDelay(60L + (i * 45L))
                    .setDuration(260L)
                    .setInterpolator(new OvershootInterpolator(0.72F))
                    .start();
        }
        View panel = findViewById(R.id.menuPanel);
        if (panel != null) {
            panel.setScaleX(0.985F);
            panel.setScaleY(0.985F);
            panel.animate()
                    .scaleX(1F)
                    .scaleY(1F)
                    .setDuration(360L)
                    .setInterpolator(new OvershootInterpolator(0.42F))
                    .start();
        }
    }

    private final View.OnTouchListener menuButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.975F).scaleY(0.975F).setDuration(70L).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1F).scaleY(1F).setDuration(120L).start();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    public void openPlayOptions(View view) {
        playMenuTap();
        GameAnalytics.get().trackMenuPlayClicked("vs_bot");
        if (checkContinueGame()) {
            showPlayChoiceDialog();
        } else {
            showNewGameDialog();
        }
    }

    private void showPlayChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View choiceView = getLayoutInflater().inflate(R.layout.play_choice_dialog, null);
        builder.setView(choiceView);
        AlertDialog choiceDialog = builder.create();

        choiceView.findViewById(R.id.choiceContinue).setOnClickListener(v -> {
            playMenuTap();
            choiceDialog.dismiss();
            continueSavedGame();
        });
        choiceView.findViewById(R.id.choiceNewGame).setOnClickListener(v -> {
            playMenuTap();
            choiceDialog.dismiss();
            showNewGameDialog();
        });

        choiceDialog.show();
        Window dialogWindow = choiceDialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    //Method called when new game is chosen
    public void startNewGame(View view) {
        playMenuTap();
        showNewGameDialog();
    }

    private void showNewGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_single_player, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // --- Theme selection (thumbnails) ---
        final int[] selectedTheme = {GamePreferences.getBoardTheme(this)};
        final FrameLayout[] thumbs = {
                dialogView.findViewById(R.id.themeThumb0),
                dialogView.findViewById(R.id.themeThumb1),
                dialogView.findViewById(R.id.themeThumb2),
                dialogView.findViewById(R.id.themeThumb3)
        };
        // Set initial selection
        updateThemeSelection(thumbs, selectedTheme[0]);
        // Click listeners
        for (int i = 0; i < thumbs.length; i++) {
            final int idx = i;
            thumbs[i].setOnClickListener(v -> {
                selectedTheme[0] = idx;
                updateThemeSelection(thumbs, idx);
            });
        }

        // --- Difficulty selection (4 buttons) ---
        final int[] selectedDiff = {GamePreferences.getBotDifficulty(this)};
        final Button[] diffBtns = {
                dialogView.findViewById(R.id.diffEasy),
                dialogView.findViewById(R.id.diffMedium),
                dialogView.findViewById(R.id.diffHard),
                dialogView.findViewById(R.id.diffRoyal)
        };
        updateDifficultySelection(diffBtns, selectedDiff[0]);
        for (int i = 0; i < diffBtns.length; i++) {
            final int idx = i;
            diffBtns[i].setOnClickListener(v -> {
                selectedDiff[0] = idx;
                updateDifficultySelection(diffBtns, idx);
            });
        }

        // --- Cancel ---
        dialogView.findViewById(R.id.singleCancel).setOnClickListener(v -> {
            playMenuTap();
            dialog.dismiss();
        });

        // --- Play ---
        dialogView.findViewById(R.id.singlePlay).setOnClickListener(v -> {
            playMenuTap();
            String playerName = ((EditText) dialogView.findViewById(R.id.singlePlayerName))
                    .getText().toString().trim();
            if (playerName.isEmpty() || playerName.equals(getString(R.string.player_name))) {
                playerName = getString(R.string.player_one);
            }
            GamePreferences.saveSelections(MenuActivity.this, selectedDiff[0], selectedTheme[0]);
            dialog.dismiss();
            File file = new File(getFilesDir().getAbsolutePath(), GAME_CONTINUE_SAVE_FILE_NAME);
            file.delete();
            Intent intent = new Intent(MenuActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_PLAYER1_NAME, playerName);
            intent.putExtra(EXTRA_PLAYER2_NAME, getString(R.string.bot_player));
            intent.putExtra(EXTRA_PLAYER1_KIND, "Player");
            intent.putExtra(EXTRA_PLAYER2_KIND, "Bot");
            startActivityForResult(intent, REQUEST_CODE_GAME);
        });

        // --- Player name: inline editable behavior ---
        EditText nameField = dialogView.findViewById(R.id.singlePlayerName);
        setupInlineEditText(nameField, getString(R.string.player_one));
        // Clear focus when clicking elsewhere
        dialogView.setOnTouchListener((v, event) -> {
            nameField.clearFocus();
            return false;
        });

        dialog.show();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int dialogWidth = (int) (displayMetrics.widthPixels * 0.88F);
            dialogWindow.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void updateThemeSelection(FrameLayout[] thumbs, int selected) {
        for (int i = 0; i < thumbs.length; i++) {
            thumbs[i].setBackgroundResource(i == selected
                    ? R.drawable.bg_theme_thumb_selected
                    : R.drawable.bg_theme_thumb_normal);
            thumbs[i].setAlpha(i == selected ? 1.0f : 0.6f);
        }
    }

    private void updateDifficultySelection(Button[] btns, int selected) {
        for (int i = 0; i < btns.length; i++) {
            if (i == selected) {
                btns[i].setBackgroundResource(R.drawable.bg_difficulty_selected);
                btns[i].setTextColor(0xFFE6A100); // Gold text for selected
            } else {
                btns[i].setBackgroundResource(R.drawable.bg_difficulty_normal);
                btns[i].setTextColor(0xFF7A8A99); // Muted blue-gray for unselected
            }
        }
    }

    //Method called when settings is chosen
    public void OpenSettings(View view) {
        playMenuTap();
        GameAnalytics.get().trackSettingsOpened();
        //Create and start settings activity
        Intent intent= new Intent(MenuActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void startTutorial(View view) {
        playMenuTap();
        GameAnalytics.get().trackTutorialClicked();
        File file=new File(MenuActivity.this.getFilesDir().getAbsolutePath(),
                MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        file.delete();
        Intent intent=new Intent(MenuActivity.this, GameActivity.class);
        intent.putExtra(EXTRA_PLAYER1_NAME, getString(R.string.tutorial_player));
        intent.putExtra(EXTRA_PLAYER2_NAME, getString(R.string.tutorial_bot));
        intent.putExtra(EXTRA_PLAYER1_KIND, "Player");
        intent.putExtra(EXTRA_PLAYER2_KIND, "Bot");
        intent.putExtra(EXTRA_TUTORIAL_MODE, true);
        startActivityForResult(intent, REQUEST_CODE_GAME);
    }

    //Method called when Pass & Play is chosen from the menu
    public void openPassAndPlay(View view) {
        playMenuTap();
        GameAnalytics.get().trackMenuPlayClicked("pass_and_play");
        showPassAndPlayDialog();
    }

    private void showPassAndPlayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.pass_and_play_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // --- Theme selection (thumbnails) ---
        final int[] selectedTheme = {GamePreferences.getBoardTheme(this)};
        final FrameLayout[] thumbs = {
                dialogView.findViewById(R.id.pnpThemeThumb0),
                dialogView.findViewById(R.id.pnpThemeThumb1),
                dialogView.findViewById(R.id.pnpThemeThumb2),
                dialogView.findViewById(R.id.pnpThemeThumb3)
        };
        updateThemeSelection(thumbs, selectedTheme[0]);
        for (int i = 0; i < thumbs.length; i++) {
            final int idx = i;
            thumbs[i].setOnClickListener(v -> {
                selectedTheme[0] = idx;
                updateThemeSelection(thumbs, idx);
            });
        }

        // --- Player names: inline edit ---
        EditText name1 = dialogView.findViewById(R.id.pnpName1);
        EditText name2 = dialogView.findViewById(R.id.pnpName2);
        setupInlineEditText(name1, getString(R.string.pass_and_play_player1_default));
        setupInlineEditText(name2, getString(R.string.pass_and_play_player2_default));

        // --- Cancel ---
        dialogView.findViewById(R.id.pnpCancel).setOnClickListener(v -> {
            playMenuTap();
            dialog.dismiss();
        });

        // --- Play ---
        dialogView.findViewById(R.id.pnpPlay).setOnClickListener(v -> {
            playMenuTap();
            String playerName1 = name1.getText().toString().trim();
            String playerName2 = name2.getText().toString().trim();
            if (playerName1.isEmpty()) playerName1 = getString(R.string.pass_and_play_player1_default);
            if (playerName2.isEmpty()) playerName2 = getString(R.string.pass_and_play_player2_default);

            GamePreferences.saveSelections(MenuActivity.this,
                    GamePreferences.getBotDifficulty(MenuActivity.this),
                    selectedTheme[0]);
            dialog.dismiss();
            File file = new File(getFilesDir().getAbsolutePath(), GAME_CONTINUE_SAVE_FILE_NAME);
            file.delete();
            Intent intent = new Intent(MenuActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_PLAYER1_NAME, playerName1);
            intent.putExtra(EXTRA_PLAYER2_NAME, playerName2);
            intent.putExtra(EXTRA_PLAYER1_KIND, "Player");
            intent.putExtra(EXTRA_PLAYER2_KIND, "Player");
            intent.putExtra(EXTRA_GAME_MODE, GAME_MODE_PASS_AND_PLAY);
            startActivityForResult(intent, REQUEST_CODE_GAME);
        });

        dialog.show();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int dialogWidth = (int) (displayMetrics.widthPixels * 0.88F);
            dialogWindow.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void setupInlineEditText(EditText field, String defaultText) {
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String text = field.getText().toString();
                if (text.equals(defaultText)) {
                    field.setText("");
                }
                field.setBackgroundResource(R.drawable.bg_name_field_focused);
            } else {
                if (field.getText().toString().trim().isEmpty()) {
                    field.setText(defaultText);
                }
                field.setBackgroundResource(R.drawable.bg_name_field_normal);
            }
        });
    }

    public void toggleLanguage(View view) {
        playMenuTap();
        // Track the language we're switching TO
        String currentLang = GamePreferences.getLanguage(this);
        String newLang = "en".equals(currentLang) ? "fa" : "en";
        GameAnalytics.get().trackLanguageChanged(newLang);
        GamePreferences.toggleLanguage(this);
        recreate();
    }

    //Method called when continue game is chosen
    public void continueGame(View view) {
        playMenuTap();
        continueSavedGame();
    }

    private void continueSavedGame() {
        //If save file exists start game activity with saved model
        if(checkContinueGame()){
            Intent intent= new Intent(MenuActivity.this, GameActivity.class);
            startActivityForResult(intent, REQUEST_CODE_GAME);
        }
    }

    //Method called when scores is chosen
    public void scores(View view) {
        playMenuTap();
        GameAnalytics.get().trackScoresOpened();
        //Create and start scores activity
        Intent intent= new Intent(MenuActivity.this, ScoresActivity.class);
        startActivity(intent);
    }

    //Method called to check if save file exists
    public boolean checkContinueGame(){
        //Open file and check
        File file=new File(this.getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        if(file.exists()){
            return true;
        }
        return false;
    }

    //Method called to set continue button color depending on save file
    public void checkAndChangeButtonColor(){
        //The main Play button now routes to continue/new-game choices when a save exists.
    }

    private void playMenuTap() {
        MenuAudioManager.get().playClick();
    }

    @Override
    protected void onDestroy() {
        if(gameAudio!=null){
            gameAudio.release();
            gameAudio=null;
        }
        super.onDestroy();
    }

    //Method called on return from finished child activity
    //  will be called when activity started with startActivityForResult(...
    //  will not be called when activity started with startActivity(...
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Check request code
        switch(requestCode){
            //If request code equals with start game code
            case REQUEST_CODE_GAME:
                //Check result code
                switch(resultCode){
                    //If result code is back pressed
                    case GAME_PRESSED_BACK:
                        //Set color of continue button
                        checkAndChangeButtonColor();
                        break;
                    //If result code is player won
                    case GAME_ENDED_OK:
                        //Get data of which player won
                        Bundle extras = data.getExtras();
                        //Set color of continue button
                        checkAndChangeButtonColor();
                        if(extras!=null) {
                            String p1Name = extras.getString(EXTRA_PLAYER1_NAME);
                            String p2Name = extras.getString(EXTRA_PLAYER2_NAME);
                            // Statistics and ads are now handled in GameActivity.onGameFinished()
                            // Here we only launch the results history viewer
                            Intent intent=new Intent(MenuActivity.this,
                                    ResultsActivity.class);
                            intent.putExtra(EXTRA_PLAYER1_NAME, p1Name);
                            intent.putExtra(EXTRA_PLAYER2_NAME, p2Name);
                            startActivity(intent);
                        }
                        break;
                }
                break;
        }
    }
}
