package games.mrlaki5.backgammon.Monetization.ads;

/**
 * Documents and enforces ad placement rules.
 *
 * ALLOWED placements:
 * ┌──────────────────────────────────────────────────────────┐
 * │ Interstitial                                             │
 * │ • After game ends, before results screen                 │
 * │ • Max frequency: 1 per 3 completed games                │
 * │ • Never during gameplay                                  │
 * ├──────────────────────────────────────────────────────────┤
 * │ Rewarded Video                                           │
 * │ • "Get Hint" button — suggests best move                 │
 * │ • Only shown when user explicitly requests it            │
 * │ • No forced viewing                                      │
 * └──────────────────────────────────────────────────────────┘
 *
 * FORBIDDEN placements:
 * ✗ Banner ads on game screen (causes negative reviews)
 * ✗ Interstitial mid-turn or mid-game
 * ✗ Forced ad viewing before game starts
 * ✗ Any ad that interrupts active gameplay
 *
 * Integration points in code:
 * - MenuActivity.onActivityResult (GAME_ENDED_OK) → adManager.showInterstitialIfReady()
 * - GameActivity: "Hint" button → adManager.showRewardedAd() → reveal best move
 */
public final class AdPlacementPolicy {
    private AdPlacementPolicy() {}

    /** Interstitial shows every N games. */
    public static final int INTERSTITIAL_EVERY_N_GAMES = 3;

    /** Rewarded ad is NEVER forced — user must tap a button to opt in. */
    public static final boolean REWARDED_IS_OPT_IN = true;

    /** Banner ads are NEVER shown. */
    public static final boolean BANNERS_ALLOWED = false;
}
