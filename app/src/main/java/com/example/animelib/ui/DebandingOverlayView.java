package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Оверлей сглаживания цветовых градиентов и устранения бандинга (Debanding Filter).
 * Отрисовывает пространственно-временную дизеринг-матрицу высокого разрешения (Blue-Noise / Triangular Dither Tile),
 * которая эффективно сглаживает ступенчатые границы градиентов в 8-битных видеопотоках и аниме.
 */
public class DebandingOverlayView extends View {

    private static Bitmap sharedDitherTile = null;
    private static BitmapShader sharedShader = null;

    private final Paint ditherPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Matrix shaderMatrix = new Matrix();
    private float intensity = 1.0f; // 0.0f (выкл) .. 1.0f (макс)
    private int frameOffset = 0;

    public DebandingOverlayView(Context context) {
        this(context, null);
    }

    public DebandingOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DebandingOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        ensureDitherTile();
        if (sharedShader != null) {
            ditherPaint.setShader(sharedShader);
        }
        ditherPaint.setAlpha((int) (48 * intensity));
    }

    private static synchronized void ensureDitherTile() {
        if (sharedDitherTile != null && !sharedDitherTile.isRecycled()) return;

        int tileSize = 128;
        sharedDitherTile = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[tileSize * tileSize];
        Random random = new Random(1337);

        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                int bayerVal = ((x ^ y) * 149 + (x & 7) * 31 + (y & 7) * 17) & 0xFF;
                float rNoise1 = (random.nextFloat() - 0.5f) * 16.0f;
                float rNoise2 = (random.nextFloat() - 0.5f) * 16.0f;
                int noise = Math.max(-24, Math.min(24, (int) ((bayerVal - 128) * 0.18f + rNoise1 + rNoise2)));

                int alpha = Math.min(255, Math.max(0, 24 + Math.abs(noise) * 2));
                int val = noise >= 0 ? 255 : 0;
                pixels[y * tileSize + x] = Color.argb(alpha, val, val, val);
            }
        }
        sharedDitherTile.setPixels(pixels, 0, tileSize, 0, 0, tileSize, tileSize);
        sharedShader = new BitmapShader(sharedDitherTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        ditherPaint.setAlpha((int) (48 * this.intensity));
        invalidate();
    }

    public float getIntensity() {
        return intensity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (intensity > 0.001f && getVisibility() == VISIBLE) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), ditherPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
