package games.mrlaki5.backgammon.Menus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import games.mrlaki5.backgammon.LocaleHelper;
import games.mrlaki5.backgammon.R;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION_MS = 3400L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean openedMenu = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySelectedLocale(newBase));
    }

    private final Runnable openMenuRunnable = new Runnable() {
        @Override
        public void run() {
            openMenu();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        ImageView studioLogo = findViewById(R.id.studioLogo);
        studioLogo.setAlpha(0F);
        studioLogo.setScaleX(0.985F);
        studioLogo.setScaleY(0.985F);
        studioLogo.animate()
                .alpha(1F)
                .scaleX(1F)
                .scaleY(1F)
                .setDuration(520L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        handler.postDelayed(openMenuRunnable, SPLASH_DURATION_MS);
    }

    private void openMenu() {
        if (openedMenu) {
            return;
        }
        openedMenu = true;
        View root = findViewById(R.id.splashRoot);
        root.animate()
                .alpha(0F)
                .setDuration(220L)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashActivity.this, MenuActivity.class));
                        finish();
                        overridePendingTransition(android.R.anim.fade_in,
                                android.R.anim.fade_out);
                    }
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMenuRunnable);
        super.onDestroy();
    }
}
