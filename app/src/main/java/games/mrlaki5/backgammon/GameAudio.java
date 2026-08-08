package games.mrlaki5.backgammon;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

import games.mrlaki5.backgammon.Menus.SettingsActivity;

public class GameAudio {
    public static final int EFFECT_MENU_TAP = 1;
    public static final int EFFECT_CHECKER_MOVE = 2;
    public static final int EFFECT_CHECKER_HIT = 3;

    private final Context context;
    private final SoundPool soundPool;
    private final int menuTapSound;
    private final int checkerMoveSound;
    private final int checkerHitSound;

    public GameAudio(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build();
        menuTapSound = soundPool.load(this.context, R.raw.menu_tap, 1);
        checkerMoveSound = soundPool.load(this.context, R.raw.checker_move, 1);
        checkerHitSound = soundPool.load(this.context, R.raw.checker_hit, 1);
    }

    public void play(int effect) {
        SharedPreferences preferences = GamePreferences.preferences(context);
        boolean soundEnabled = preferences.getBoolean(SettingsActivity.KEY_SOUND_ENABLED,
                SettingsActivity.DEF_SOUND_ENABLED);
        boolean effectsEnabled = preferences.getBoolean(SettingsActivity.KEY_EFFECTS_ENABLED,
                SettingsActivity.DEF_EFFECTS_ENABLED);
        int soundVolume = preferences.getInt(SettingsActivity.KEY_SOUND_VOLUME,
                SettingsActivity.DEF_SOUND_VOLUME);
        if (!soundEnabled || !effectsEnabled || soundVolume <= 0) {
            return;
        }

        int soundId;
        switch (effect) {
            case EFFECT_CHECKER_MOVE:
                soundId = checkerMoveSound;
                break;
            case EFFECT_CHECKER_HIT:
                soundId = checkerHitSound;
                break;
            default:
                soundId = menuTapSound;
                break;
        }
        float volume = Math.max(0F, Math.min(1F, soundVolume / 100F));
        soundPool.play(soundId, volume, volume, 1, 0, 1F);
    }

    public void release() {
        soundPool.release();
    }
}
