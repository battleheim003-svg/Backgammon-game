package games.mrlaki5.backgammon;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import games.mrlaki5.backgammon.Analytics.AnalyticsProvider;
import games.mrlaki5.backgammon.Analytics.CrashReporter;
import games.mrlaki5.backgammon.Analytics.FirebaseAnalyticsProvider;
import games.mrlaki5.backgammon.Analytics.GameAnalytics;
import games.mrlaki5.backgammon.Analytics.StubAnalyticsProvider;
import games.mrlaki5.backgammon.Analytics.StubCrashReporter;

import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.AdNetworkError;

/**
 * Custom Application class for global SDK initialization.
 *
 * Initialization order:
 * 1. TapsellPlus (ads)
 * 2. Analytics (Firebase or Stub)
 * 3. Audio
 */
public class BackgammonApp extends Application {

    private static final String TAG = "BackgammonApp";

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch_done";

    private boolean analyticsInitialized = false;
    private int activeActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize TapsellPlus SDK
        TapsellPlus.setDebugMode(Log.DEBUG);
        TapsellPlus.initialize(this, BuildConfig.TAPSELL_APP_KEY, new TapsellPlusInitListener() {
            @Override
            public void onInitializeSuccess(AdNetworks adNetworks) {
                Log.d(TAG, "TapsellPlus initialized successfully");
            }

            @Override
            public void onInitializeFailed(AdNetworks adNetworks, AdNetworkError adNetworkError) {
                Log.w(TAG, "TapsellPlus init failed: " + adNetworkError.getErrorMessage());
            }
        });

        // Initialize menu audio (click + music)
        MenuAudioManager.get().init(this);

        // Register activity lifecycle for analytics initialization and session tracking
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (!analyticsInitialized) {
                    initializeAnalytics(activity);
                    analyticsInitialized = true;
                }
            }

            @Override public void onActivityStarted(Activity activity) {
                activeActivityCount++;
                if (activeActivityCount == 1) {
                    // App came to foreground — session starts
                    GameAnalytics.get().trackSessionStart();
                }
            }

            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}

            @Override public void onActivityStopped(Activity activity) {
                activeActivityCount--;
                if (activeActivityCount == 0) {
                    // All activities stopped — app went to background — session ends
                    GameAnalytics.get().trackSessionEnd();
                }
            }

            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void initializeAnalytics(Activity activity) {
        // Use Firebase Analytics as the real provider
        // Falls back gracefully if google-services.json is missing
        AnalyticsProvider provider;
        try {
            provider = new FirebaseAnalyticsProvider();
            Log.d(TAG, "Using Firebase Analytics provider");
        } catch (Exception e) {
            Log.w(TAG, "Firebase unavailable, using stub analytics", e);
            provider = new StubAnalyticsProvider();
        }

        CrashReporter crashReporter = new StubCrashReporter();

        GameAnalytics.init(activity, provider, crashReporter);

        // Track app open
        GameAnalytics.get().trackAppOpen();

        // Track first launch (one-time)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_FIRST_LAUNCH, false)) {
            GameAnalytics.get().trackFirstLaunch();
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply();
        }
    }
}
