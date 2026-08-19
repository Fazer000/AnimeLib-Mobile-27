package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Оверлей сглаживания цветовых градиентов и устранения бандинга (Debanding Filter).
 * Отрисовывает высокочастотную пространственную дизеринг-матрицу (Blue-Noise dither tile),
 * которая предотвращает визуальные ступени квантования цветов в 8-битных видеопотоках.
 */
public class DebandingOverlayView extends View {

    private final Paint ditherPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private Bitmap ditherTile;
    private float intensity = 1.0f; // 0.0f (выкл) .. 1.0f (макс)

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
        generateDitherTile();
    }

    /**
     * Генерация 128x128 симулятора синего шума (High-Pass Blue Noise Tile)
     */
    private void generateDitherTile() {
        int tileSize = 128;
        ditherTile = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[tileSize * tileSize];
        Random random = new Random(42); // Фиксированный сид для детерминированного равномерного шума

        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                // Использовать байеровскую пространственную матрицу + гауссов шум
                int bayerVal = ((x ^ y) * 149 + (x & 3) * 31 + (y & 3) * 17) & 0xFF;
                float rNoise = (random.nextFloat() - 0.5f) * 12.0f;
                int noise = Math.max(-20, Math.min(20, (int) ((bayerVal - 128) * 0.15f + rNoise)));

                int alpha = Math.min(255, Math.max(0, 16 + Math.abs(noise)));
                int val = noise >= 0 ? 255 : 0;
                pixels[y * tileSize + x] = Color.argb(alpha, val, val, val);
            }
        }
        ditherTile.setPixels(pixels, 0, tileSize, 0, 0, tileSize, tileSize);

        BitmapShader shader = new BitmapShader(ditherTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        ditherPaint.setShader(shader);
        ditherPaint.setAlpha((int) (22 * intensity)); // Мягкая прозрачность для сглаживания ступенек без ухудшения четкости
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        ditherPaint.setAlpha((int) (22 * this.intensity));
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
        if (ditherTile != null && !ditherTile.isRecycled()) {
            ditherTile.recycle();
            ditherTile = null;
        }
    }
}
