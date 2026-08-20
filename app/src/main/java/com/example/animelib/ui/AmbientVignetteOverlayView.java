package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Overlay View для фоновой подсветки (Ambilight).
 * Использует Alpha Masking (DST_OUT), чтобы плавно снижать прозрачность подсветки
 * к краям экрана (~15% от изначальной яркости) и убирать её под плеером в портретном режиме.
 * НЕ рисует никаких черных мазков и серых теней, сохраняя 100% чистый, насыщенный цвет свечения.
 */
public class AmbientVignetteOverlayView extends View {

    private final Paint eraseBottomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint topEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint bottomEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint leftEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint rightEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Paint portraitBottomFadePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final RectF videoBounds = new RectF();
    private boolean hasVideoBounds = false;
    private float sidePanelLeft = -1f;

    private int lastWidth = 0;
    private int lastHeight = 0;

    // ГРАДИЕНТ ПРОЗРАЧНОСТИ ДЛЯ DST_OUT:
    // 0x00000000 = 0% вычитания прозрачности (100% сочный яркий цвет)
    // 0xFF000000 = 100% вычитания (полное погашение к самым внешним краям экрана)
    private static final int[] ALPHA_FALLOFF_TO_EDGE = new int[]{
            0x00000000,
            0x1A000000,
            0x4D000000,
            0x80000000,
            0xCC000000,
            0xFF000000  // Плавно уходит в 0% яркости у самого края
    };

    // Градиент для ухода подсветки в ноль под видеоплеером (портретный режим / открытые боковые окна)
    private static final int[] ALPHA_FALLOFF_PORTRAIT_BOTTOM = new int[]{
            0x00000000, // 100% яркость подсветки
            0x33000000,
            0x80000000,
            0xCC000000,
            0xFF000000  // 100% убираем подсветку
    };

    private static final float[] FALLOFF_STOPS = new float[]{
            0.0f, 0.20f, 0.45f, 0.70f, 0.88f, 1.0f
    };

    private static final float[] PORTRAIT_STOPS = new float[]{
            0.0f, 0.25f, 0.55f, 0.80f, 1.0f
    };

    public AmbientVignetteOverlayView(Context context) {
        this(context, null);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        PorterDuffXfermode dstOutMode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

        topEdgePaint.setXfermode(dstOutMode);
        bottomEdgePaint.setXfermode(dstOutMode);
        leftEdgePaint.setXfermode(dstOutMode);
        rightEdgePaint.setXfermode(dstOutMode);

        portraitBottomFadePaint.setXfermode(dstOutMode);

        eraseBottomPaint.setColor(0xFF000000);
        eraseBottomPaint.setXfermode(dstOutMode);
        eraseBottomPaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_NONE, null);
    }

    public void setVideoBounds(float left, float top, float right, float bottom) {
        setVideoBounds(left, top, right, bottom, -1f);
    }

    public void setVideoBounds(float left, float top, float right, float bottom, float sidePanelLeft) {
        if (!hasVideoBounds || videoBounds.left != left || videoBounds.top != top ||
                videoBounds.right != right || videoBounds.bottom != bottom ||
                this.sidePanelLeft != sidePanelLeft) {
            videoBounds.set(left, top, right, bottom);
            this.sidePanelLeft = sidePanelLeft;
            hasVideoBounds = true;
            rebuildShaders();
            invalidate();
        }
    }

    public void clearCustomVideoBounds() {
        if (hasVideoBounds || sidePanelLeft > 0) {
            hasVideoBounds = false;
            this.sidePanelLeft = -1f;
            rebuildShaders();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;
            rebuildShaders();
        }
    }

    private void rebuildShaders() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (hasVideoBounds) {
            float vTop = videoBounds.top;
            float vBottom = videoBounds.bottom;
            float vLeft = videoBounds.left;
            float vRight = videoBounds.right;

            // Сверху над видео (кверху экрана)
            float topDepth = dpToPx(24);
            topEdgePaint.setShader(new LinearGradient(
                    0, vTop, 0, Math.max(0, vTop - topDepth),
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            // Под видеоплеером в портретном режиме (плавный уход свечения на 100dp ниже видео)
            float bottomStart = vBottom + dpToPx(20);
            float bottomEnd = vBottom + dpToPx(90);
            portraitBottomFadePaint.setShader(new LinearGradient(
                    0, bottomStart, 0, bottomEnd,
                    ALPHA_FALLOFF_PORTRAIT_BOTTOM, PORTRAIT_STOPS, Shader.TileMode.CLAMP
            ));

            // Слева от видео
            if (vLeft > 0) {
                leftEdgePaint.setShader(new LinearGradient(
                        vLeft, 0, 0, 0,
                        ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
                ));
            }

            // Справа от видео (с учетом бокового окна, если оно открыто)
            if (sidePanelLeft > 0 && sidePanelLeft < w) {
                float fadeStart = Math.max(vRight, sidePanelLeft - dpToPx(40));
                rightEdgePaint.setShader(new LinearGradient(
                        fadeStart, 0, sidePanelLeft, 0,
                        ALPHA_FALLOFF_PORTRAIT_BOTTOM, PORTRAIT_STOPS, Shader.TileMode.CLAMP
                ));
            } else if (vRight < w) {
                rightEdgePaint.setShader(new LinearGradient(
                        vRight, 0, w, 0,
                        ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
                ));
            }
        } else {
            // Горизонтальный (полноэкранный) режим:
            // Плавное, сочное затухание подсветки к 4 внешним границам экрана
            float edgeDepthX = dpToPx(80); // Мягкий уход к левому/правому краю (80dp)
            float edgeDepthY = dpToPx(40); // Мягкий уход к верхнему/нижнему краю (40dp)

            topEdgePaint.setShader(new LinearGradient(
                    0, edgeDepthY, 0, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            bottomEdgePaint.setShader(new LinearGradient(
                    0, h - edgeDepthY, 0, h,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            leftEdgePaint.setShader(new LinearGradient(
                    edgeDepthX, 0, 0, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            rightEdgePaint.setShader(new LinearGradient(
                    w - edgeDepthX, 0, w, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int saveCount = canvas.saveLayer(0, 0, w, h, null);

        if (hasVideoBounds) {
            float vTop = videoBounds.top;
            float vBottom = videoBounds.bottom;
            float vLeft = videoBounds.left;
            float vRight = videoBounds.right;

            // Сверху над видео
            if (vTop > 0) {
                float topDepth = dpToPx(24);
                canvas.drawRect(0, Math.max(0, vTop - topDepth), w, vTop, topEdgePaint);
            }

            // Снизу под видео: плавный уход свечения
            float bottomStart = vBottom + dpToPx(20);
            float bottomEnd = vBottom + dpToPx(90);
            if (bottomEnd < h) {
                canvas.drawRect(0, bottomStart, w, bottomEnd, portraitBottomFadePaint);
                // Полностью стираем ниже 90dp
                canvas.drawRect(0, bottomEnd, w, h, eraseBottomPaint);
            } else {
                canvas.drawRect(0, bottomStart, w, h, portraitBottomFadePaint);
            }

            // Слева от видео
            if (vLeft > 0) {
                canvas.drawRect(0, 0, vLeft, h, leftEdgePaint);
            }

            // Справа от видео: гасим подсветку под открытым боковым окном
            if (sidePanelLeft > 0 && sidePanelLeft < w) {
                float fadeStart = Math.max(vRight, sidePanelLeft - dpToPx(40));
                canvas.drawRect(fadeStart, 0, sidePanelLeft, h, rightEdgePaint);
                canvas.drawRect(sidePanelLeft, 0, w, h, eraseBottomPaint);
            } else if (vRight < w) {
                canvas.drawRect(vRight, 0, w, h, rightEdgePaint);
            }
        } else {
            // Горизонтальный (полноэкранный) режим:
            // Мягкое затухание подсветки к 4 внешним краям экрана
            float edgeDepthX = dpToPx(80);
            float edgeDepthY = dpToPx(40);

            // Верхний край
            canvas.drawRect(0, 0, w, edgeDepthY, topEdgePaint);

            // Нижний край
            canvas.drawRect(0, h - edgeDepthY, w, h, bottomEdgePaint);

            // Левый край
            canvas.drawRect(0, 0, edgeDepthX, h, leftEdgePaint);

            // Правый край
            canvas.drawRect(w - edgeDepthX, 0, w, h, rightEdgePaint);
        }

        canvas.restoreToCount(saveCount);
    }
}

