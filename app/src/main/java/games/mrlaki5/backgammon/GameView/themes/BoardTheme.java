package games.mrlaki5.backgammon.GameView.themes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Strategy interface defining visual appearance and rendering algorithms for board themes.
 */
public interface BoardTheme {
    int getThemeId();
    int getBackgroundDrawableRes();

    void drawChip(Canvas canvas, RectF rect, int player,
                  Paint fillPaint, Paint rimPaint, Paint shadowPaint,
                  Paint highlightPaint, Paint specPaint, Paint bevelPaint,
                  Paint accentPaint, Paint dotPaint, Paint glowPaint, Paint contactShadowPaint,
                  RectF shadowRect, RectF specRect);

    void drawEndChip(Canvas canvas, RectF rect, int player,
                     Paint fillPaint, Paint rimPaint, Paint shadowPaint,
                     Paint highlightPaint, Paint glowPaint, Paint contactShadowPaint,
                     RectF shadowRect);
}
