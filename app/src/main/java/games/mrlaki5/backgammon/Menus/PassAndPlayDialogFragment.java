package games.mrlaki5.backgammon.Menus;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.TypefaceCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;

import games.mrlaki5.backgammon.GameControllers.GameActivity;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.R;

public class PassAndPlayDialogFragment extends BottomSheetDialogFragment {

    private final int[] selectedTheme = new int[1];

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                int displayHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight(displayHeight);

                float translateY = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 80f, getResources().getDisplayMetrics());
                bottomSheet.setTranslationY(translateY);
                bottomSheet.setAlpha(0f);
                bottomSheet.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(280L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pass_and_play_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Custom font for title
        TextView tvTitle = view.findViewById(R.id.pnpSheetTitle);
        if (tvTitle != null) {
            try {
                Typeface base = Typeface.createFromAsset(requireContext().getAssets(), "fonts/Cinzel-Regular.ttf");
                Typeface typeface = TypefaceCompat.create(requireContext(), base, Typeface.BOLD);
                tvTitle.setTypeface(typeface);
            } catch (Exception ignored) {}
        }

        // Theme selection (2x4 thumbnails)
        selectedTheme[0] = GamePreferences.getBoardTheme(requireContext());
        setupThemePicker(view, selectedTheme);

        // Player Names
        EditText name1 = view.findViewById(R.id.pnpName1);
        EditText name2 = view.findViewById(R.id.pnpName2);
        if (name1 != null) {
            MenuActivity.setupInlineEditText(name1, getString(R.string.pass_and_play_player1_default));
        }
        if (name2 != null) {
            MenuActivity.setupInlineEditText(name2, getString(R.string.pass_and_play_player2_default));
        }

        // Action buttons
        MaterialButton btnCancel = view.findViewById(R.id.pnpCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (getActivity() instanceof MenuActivity) {
                    ((MenuActivity) getActivity()).playMenuTap();
                }
                dismiss();
            });
        }

        MaterialButton btnPlay = view.findViewById(R.id.pnpPlay);
        if (btnPlay != null) {
            applyPlayButtonPolish(btnPlay);
            btnPlay.setOnClickListener(v -> {
                if (getActivity() instanceof MenuActivity) {
                    ((MenuActivity) getActivity()).playMenuTap();
                }
                String playerName1 = name1 != null ? name1.getText().toString().trim() : "";
                String playerName2 = name2 != null ? name2.getText().toString().trim() : "";
                if (playerName1.isEmpty()) playerName1 = getString(R.string.pass_and_play_player1_default);
                if (playerName2.isEmpty()) playerName2 = getString(R.string.pass_and_play_player2_default);

                GamePreferences.saveSelections(requireContext(),
                        GamePreferences.getBotDifficulty(requireContext()),
                        selectedTheme[0]);
                dismiss();

                File file = new File(requireActivity().getFilesDir().getAbsolutePath(),
                        MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
                file.delete();

                Intent intent = new Intent(requireContext(), GameActivity.class);
                intent.putExtra(MenuActivity.EXTRA_PLAYER1_NAME, playerName1);
                intent.putExtra(MenuActivity.EXTRA_PLAYER2_NAME, playerName2);
                intent.putExtra(MenuActivity.EXTRA_PLAYER1_KIND, "Player");
                intent.putExtra(MenuActivity.EXTRA_PLAYER2_KIND, "Player");
                intent.putExtra(MenuActivity.EXTRA_GAME_MODE, MenuActivity.GAME_MODE_PASS_AND_PLAY);
                requireActivity().startActivityForResult(intent, MenuActivity.REQUEST_CODE_GAME);
            });
        }
    }

    private void applyPlayButtonPolish(View btnPlay) {
        btnPlay.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70L).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120L)
                            .setInterpolator(new OvershootInterpolator(0.72f)).start();
                    break;
            }
            return false;
        });

        btnPlay.setAlpha(0f);
        btnPlay.setTranslationY(18f);
        btnPlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .setInterpolator(new OvershootInterpolator(0.72f))
                .start();
    }

    private void setupThemePicker(View root, int[] selectedRef) {
        int[] thumbIds = {
                R.id.pnpThemeThumb0, R.id.pnpThemeThumb1, R.id.pnpThemeThumb2, R.id.pnpThemeThumb3,
                R.id.pnpThemeThumb4, R.id.pnpThemeThumb5, R.id.pnpThemeThumb6, R.id.pnpThemeThumb7
        };
        int[] overlayIds = {
                0, R.id.pnpLockOverlay1, R.id.pnpLockOverlay2, R.id.pnpLockOverlay3,
                R.id.pnpLockOverlay4, 0, 0, 0
        };

        FrameLayout[] thumbs = new FrameLayout[thumbIds.length];
        View[] lockViews = new View[overlayIds.length];

        for (int i = 0; i < thumbIds.length; i++) {
            thumbs[i] = root.findViewById(thumbIds[i]);
        }
        for (int i = 0; i < overlayIds.length; i++) {
            if (overlayIds[i] != 0) {
                lockViews[i] = root.findViewById(overlayIds[i]);
            }
        }

        refreshThemeLocks(lockViews);
        updateThemeSelection(thumbs, selectedRef[0]);

        for (int i = 0; i < thumbs.length; i++) {
            final int idx = i;
            if (thumbs[i] == null) continue;
            thumbs[i].setOnClickListener(v -> {
                if (idx >= 5) {
                    Toast.makeText(requireContext(), "این تم به‌زودی اضافه می‌شود!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (GamePreferences.isThemeUnlocked(requireContext(), idx)) {
                    selectedRef[0] = idx;
                    updateThemeSelection(thumbs, idx);
                } else {
                    if (getActivity() instanceof MenuActivity) {
                        ((MenuActivity) getActivity()).showThemeUnlockAd(idx, () -> {
                            GamePreferences.unlockTheme(requireContext(), idx);
                            refreshThemeLocks(lockViews);
                            selectedRef[0] = idx;
                            updateThemeSelection(thumbs, idx);
                            Toast.makeText(requireContext(),
                                    R.string.theme_unlocked_toast,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    private void refreshThemeLocks(View[] lockViews) {
        for (int i = 0; i < lockViews.length; i++) {
            if (lockViews[i] != null) {
                lockViews[i].setVisibility(
                        GamePreferences.isThemeUnlocked(requireContext(), i) ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void updateThemeSelection(FrameLayout[] thumbs, int selected) {
        for (int i = 0; i < thumbs.length; i++) {
            if (thumbs[i] == null) continue;
            boolean isSel = (i == selected);
            thumbs[i].setBackgroundResource(isSel
                    ? R.drawable.bg_theme_thumb_selected
                    : R.drawable.bg_theme_thumb_normal);
            thumbs[i].setAlpha(isSel ? 1.0f : 0.6f);
            if (!isSel) {
                thumbs[i].setScaleX(1f);
                thumbs[i].setScaleY(1f);
            }
        }
        if (selected >= 0 && selected < thumbs.length && thumbs[selected] != null) {
            View selectedThumbView = thumbs[selected];
            selectedThumbView.animate().scaleX(1.12f).scaleY(1.12f).setDuration(100)
                    .withEndAction(() -> selectedThumbView.animate().scaleX(1f).scaleY(1f)
                            .setInterpolator(new OvershootInterpolator(3f)).setDuration(150).start())
                    .start();
        }
    }
}
