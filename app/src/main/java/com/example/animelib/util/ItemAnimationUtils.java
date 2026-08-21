package com.example.animelib.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ItemAnimationUtils {

    /**
     * Анимирует нажатие на элемент списка (эпизоды, озвучки, качества, кнопки переключения):
     * плавный отклик прозрачности без изменения размера (кейлинга).
     */
    public static void animateItemClick(View view, Runnable action) {
        if (view == null) {
            if (action != null) action.run();
            return;
        }

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(0.65f);

        view.animate()
                .alpha(1.0f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (action != null) {
            action.run();
        }
    }

    /**
     * Плавный переход элемента между активным и неактивным состоянием (без скейлинга).
     */
    public static void animateItemStateTransition(View view, boolean isActive) {
        if (view == null) return;

        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        float targetAlpha = isActive ? 1.0f : 0.82f;
        view.animate().cancel();
        view.animate()
                .alpha(targetAlpha)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Плавное выделение выбранного айтема (без скейлинга).
     */
    public static void animateItemSelection(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        
        view.setAlpha(0.7f);
        view.animate()
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Анимирует смену текста/плашки при переключении эпизода или озвучки (без скейлинга)
     */
    public static void animateTextChange(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(0.3f);

        view.animate()
                .alpha(1.0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}

