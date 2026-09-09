package games.mrlaki5.backgammon.GameControllers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GameView.OnBoardImage;
import games.mrlaki5.backgammon.R;

/**
 * Game execution thread using ExecutorService for thread-safe turn handling.
 */
public class GameTask {

    // Time between two turns
    private final long sleepTime;
    // Flag for work, when set to 0 execution loop terminates
    private final AtomicInteger workFlag = new AtomicInteger(1);
    // Flag set to 1 when task finishes execution
    private final AtomicInteger finishedFlag = new AtomicInteger(0);
    // Flag for synchronization of end routines
    private final AtomicInteger endRoutineStarted = new AtomicInteger(0);

    // Single-thread executor for game loop
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Game activity context
    private final GameActivity gameActivity;
    // Model of game state
    private final Model model;
    // Object of game logic
    private final GameLogic gameLogic;
    // View
    private final OnBoardImage onBoardImage;

    // Constructor
    public GameTask(Model model, GameLogic gameLogic, OnBoardImage onBoardImage, long sleepTime,
                    GameActivity gameActivity) {
        this.model = model;
        this.gameLogic = gameLogic;
        this.onBoardImage = onBoardImage;
        this.sleepTime = (sleepTime == 0) ? 1 : sleepTime;
        this.gameActivity = gameActivity;
    }

    // Method for writing text on view
    private void writeMessage(String text) {
        writeMessage(text, false);
    }

    private void writeMessage(String text, boolean rollPrompt) {
        onBoardImage.setMessage(model.getCurrentObjectPlayer().getPlayerName() +
                ", " + text, model.getCurrentPlayer(), rollPrompt);
        onBoardImage.postInvalidate();
    }

    /**
     * Starts execution of the game loop on a background thread.
     */
    public void execute(Void... voids) {
        executor.execute(this::runGameLoop);
    }

    private void runGameLoop() {
        try {
            while (workFlag.get() == 1 && !Thread.currentThread().isInterrupted()) {
                switch (model.getState()) {
                    // State 0: Player 1 rolls one die for opening
                    case 0:
                        writeMessage(gameActivity.getString(R.string.roll_dice), true);
                        model.getCurrentObjectPlayer().actionRoll();
                        if (workFlag.get() == 0) break;

                        model.setState(1);
                        model.changeCurrentPlayer();

                        // Show turn-switch overlay in Pass & Play for initial roll
                        if (gameActivity.isPassAndPlayMode()) {
                            gameActivity.showTurnSwitchAndWait(
                                    model.getCurrentObjectPlayer().getPlayerName(),
                                    model.getCurrentPlayer());
                            if (workFlag.get() == 0) break;
                        }

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        break;

                    // State 1: Player 2 rolls one die for opening
                    case 1:
                        writeMessage(gameActivity.getString(R.string.roll_dice), true);
                        model.getCurrentObjectPlayer().actionRoll();
                        if (workFlag.get() == 0) break;

                        // Check which player got higher number, that one plays first
                        if (model.getDiceThrows()[0].getThrowNumber() >=
                                model.getDiceThrows()[1].getThrowNumber()) {
                            model.setCurrentPlayer(1);
                        } else {
                            model.setCurrentPlayer(2);
                        }

                        model.setState(2);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        break;

                    // State 2: Current player moves checkers
                    case 2:
                        model.setNextMoves(gameLogic.calculateMoves(model.getBoardFields(),
                                model.getCurrentPlayer(), model.getDiceThrows()));

                        if (!model.getNextMoves().isEmpty()) {
                            writeMessage(gameActivity.getString(R.string.move_checkers));
                            model.getCurrentObjectPlayer().actionMove();
                            if (workFlag.get() == 0) break;
                        }

                        // Check if current player finished game
                        if (gameLogic.getCurrPlayerFinished() != 0) {
                            if (endRoutineStarted.compareAndSet(0, 1)) {
                                workFlag.set(0);
                                int winningPlayer = gameLogic.getCurrPlayerFinished();
                                String p1Name = model.getPlayers()[0].getPlayerName();
                                String p2Name = model.getPlayers()[1].getPlayerName();
                                String gameMode;
                                if (gameActivity.isPassAndPlayMode()) {
                                    gameMode = MenuActivity.GAME_MODE_PASS_AND_PLAY;
                                } else {
                                    gameMode = MenuActivity.GAME_MODE_VS_BOT;
                                }
                                gameActivity.playGameFinishedEffect();
                                gameActivity.onGameFinished(winningPlayer, p1Name, p2Name, gameMode);
                            }
                            break;
                        }

                        model.changeCurrentPlayer();
                        model.setState(3);

                        // Tutorial mode: skip bot turn entirely, stay on player 1
                        if (gameActivity.isTutorialMode()) {
                            model.setCurrentPlayer(1);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            break;
                        }

                        // Show turn-switch overlay in Pass & Play mode
                        if (gameActivity.isPassAndPlayMode()) {
                            gameActivity.showTurnSwitchAndWait(
                                    model.getCurrentObjectPlayer().getPlayerName(),
                                    model.getCurrentPlayer());
                            if (workFlag.get() == 0) break;
                        }

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        break;

                    // State 3: Current player rolls dice
                    case 3:
                        writeMessage(gameActivity.getString(R.string.roll_dice), true);
                        model.getCurrentObjectPlayer().actionRoll();
                        if (workFlag.get() == 0) break;

                        model.setState(2);
                        break;
                }
            }
        } finally {
            finishedFlag.set(1);
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * Shuts down the background executor immediately.
     */
    public void shutdown() {
        workFlag.set(0);
        executor.shutdownNow();
    }

    // Getters and setters for compatibility
    public int getWorkFlag() {
        return workFlag.get();
    }

    public void setWorkFlag(int workFlag) {
        this.workFlag.set(workFlag);
    }

    public int getFinishedFlag() {
        return finishedFlag.get();
    }

    public void setFinishedFlag(int finishedFlag) {
        this.finishedFlag.set(finishedFlag);
    }

    public int getEndRoutineStarted() {
        return endRoutineStarted.get();
    }

    public void setEndRoutineStarted(int endRoutineStarted) {
        this.endRoutineStarted.set(endRoutineStarted);
    }
}
