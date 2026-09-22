package games.mrlaki5.backgammon.Journey;

import com.royalbackgammon.core.variant.Variant;

import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.R;

/** One chapter of the journey: a themed opponent, a variant and a match length. */
public enum JourneyStage {

    HOME(R.string.journey_1_title, R.string.journey_1_flavor, R.string.journey_1_opponent,
            Variant.STANDARD, GamePreferences.BOT_EASY, 1, false, 30),
    TEAHOUSE(R.string.journey_2_title, R.string.journey_2_flavor, R.string.journey_2_opponent,
            Variant.STANDARD, GamePreferences.BOT_MEDIUM, 3, false, 40),
    ISTANBUL(R.string.journey_3_title, R.string.journey_3_flavor, R.string.journey_3_opponent,
            Variant.TAVLA, GamePreferences.BOT_MEDIUM, 3, false, 50),
    ATHENS(R.string.journey_4_title, R.string.journey_4_flavor, R.string.journey_4_opponent,
            Variant.PORTES, GamePreferences.BOT_MEDIUM, 3, false, 50),
    CRETE(R.string.journey_5_title, R.string.journey_5_flavor, R.string.journey_5_opponent,
            Variant.PLAKOTO, GamePreferences.BOT_HARD, 3, false, 70),
    THESSALONIKI(R.string.journey_6_title, R.string.journey_6_flavor, R.string.journey_6_opponent,
            Variant.FEVGA, GamePreferences.BOT_HARD, 3, false, 70),
    CASPIAN(R.string.journey_7_title, R.string.journey_7_flavor, R.string.journey_7_opponent,
            Variant.NARDY, GamePreferences.BOT_HARD, 5, false, 90),
    HARBOUR(R.string.journey_8_title, R.string.journey_8_flavor, R.string.journey_8_opponent,
            Variant.ACEY_DEUCEY, GamePreferences.BOT_HARD, 1, false, 100),
    GRAND_MASTER(R.string.journey_9_title, R.string.journey_9_flavor, R.string.journey_9_opponent,
            Variant.PORTES, GamePreferences.BOT_ROYAL, 5, true, 150);

    private final int titleRes;
    private final int flavorRes;
    private final int opponentRes;
    private final Variant variant;
    private final int difficulty;
    private final int matchTarget;
    private final boolean tavliRotation;
    private final int rewardCoins;

    JourneyStage(int titleRes, int flavorRes, int opponentRes, Variant variant, int difficulty,
                 int matchTarget, boolean tavliRotation, int rewardCoins) {
        this.titleRes = titleRes;
        this.flavorRes = flavorRes;
        this.opponentRes = opponentRes;
        this.variant = variant;
        this.difficulty = difficulty;
        this.matchTarget = matchTarget;
        this.tavliRotation = tavliRotation;
        this.rewardCoins = rewardCoins;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getFlavorRes() {
        return flavorRes;
    }

    public int getOpponentRes() {
        return opponentRes;
    }

    public Variant getVariant() {
        return variant;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getMatchTarget() {
        return matchTarget;
    }

    /** The final chapter plays a full Tavli rotation. */
    public boolean isTavliRotation() {
        return tavliRotation;
    }

    public int getRewardCoins() {
        return rewardCoins;
    }
}
