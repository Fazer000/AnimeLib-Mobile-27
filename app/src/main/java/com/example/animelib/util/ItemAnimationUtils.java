package com.example.animelib.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class ItemAnimationUtils {

    /**
     * Анимирует нажатие на элемент списка (эпизоды, озвучки, качества, кнопки переключения):
     * пружинистый скейл 0.92f -> 1.0f с вызовом целевого действия.
     */
    public static void animateItemClick(View view, Runnable action) {
        if (view == null) {
            if (action != null) action.run();
            return;
        }

        view.animate().cancel();
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.92f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.92f, 1.0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(180);
        set.setInterpolator(new OvershootInterpolator(2.5f));
        set.start();

        if (action != null) {
            action.run();
        }
    }

    /**
     * Анимирует выделение выбранного айтема (активная озвучка или серия):
     * мягкое увеличенное пульсирование 1.0f -> 1.06f -> 1.0f.
     */
    public static void animateItemSelection(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.06f, 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.06f, 1.0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleUpX, scaleUpY);
        set.setDuration(220);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    /**
     * Анимирует смену текста/плашки при переключении эпизода или озвучки
     */
    public static void animateTextChange(View view) {
        if (view == null) return;

        view.animate().cancel();
        view.setAlpha(0.3f);
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);

        view.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
