package com.example.animelib.managers;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.DebandingOverlayView;

/**
 * Менеджер сглаживания градиентов и дебандинга (Debanding Engine).
 * Убирает "ступеньки" и цветовые полосы (color banding artifacts), характерные для 8-битного видео и аниме.
 * 
 * Включает:
 * 1. Аппаратный AGSL RuntimeShader (Android 13+ / API 33+) для векторной межпиксельной фильтрации градиентов;
 * 2. Высокочастотный пространственный оверлей дизеринга (DebandingOverlayView) для всех версий Android.
 */
public class DebandingManager {
    private static final String TAG = "DebandingManager";

    private static final String AGSL_DEBAND_SHADER =
            "uniform shader composer;\n" +
            "uniform float in_debandStrength;\n" +
            "float rand(float2 p) {\n" +
            "    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);\n" +
            "}\n" +
            "half4 main(float2 fragCoord) {\n" +
            "    half4 origColor = composer.eval(fragCoord);\n" +
            "    if (in_debandStrength <= 0.001) return origColor;\n" +
            "    float threshold = 0.075 * in_debandStrength;\n" +
            "    float range = 14.0 * in_debandStrength;\n" +
            "    float noiseSeed = rand(fragCoord);\n" +
            "    float angle = noiseSeed * 6.2831853;\n" +
            "    float2 dir1 = float2(cos(angle), sin(angle));\n" +
            "    float2 dir2 = float2(-dir1.y, dir1.x);\n" +
            "    half3 accumColor = origColor.rgb;\n" +
            "    float weightSum = 1.0;\n" +
            "    float2 p0 = fragCoord + dir1 * (range * 0.5);\n" +
            "    float2 p1 = fragCoord - dir1 * (range * 0.5);\n" +
            "    float2 p2 = fragCoord + dir2 * (range * 0.5);\n" +
            "    float2 p3 = fragCoord - dir2 * (range * 0.5);\n" +
            "    float2 p4 = fragCoord + dir1 * range;\n" +
            "    float2 p5 = fragCoord - dir1 * range;\n" +
            "    float2 p6 = fragCoord + dir2 * range;\n" +
            "    float2 p7 = fragCoord - dir2 * range;\n" +
            "    half3 c0 = composer.eval(p0).rgb;\n" +
            "    half3 c1 = composer.eval(p1).rgb;\n" +
            "    half3 c2 = composer.eval(p2).rgb;\n" +
            "    half3 c3 = composer.eval(p3).rgb;\n" +
            "    half3 c4 = composer.eval(p4).rgb;\n" +
            "    half3 c5 = composer.eval(p5).rgb;\n" +
            "    half3 c6 = composer.eval(p6).rgb;\n" +
            "    half3 c7 = composer.eval(p7).rgb;\n" +
            "    half3 d0 = abs(origColor.rgb - c0); float m0 = max(max(d0.r, d0.g), d0.b); float w0 = step(m0, threshold) * (1.0 - m0 / (threshold + 0.0001)); accumColor += c0 * w0; weightSum += w0;\n" +
            "    half3 d1 = abs(origColor.rgb - c1); float m1 = max(max(d1.r, d1.g), d1.b); float w1 = step(m1, threshold) * (1.0 - m1 / (threshold + 0.0001)); accumColor += c1 * w1; weightSum += w1;\n" +
            "    half3 d2 = abs(origColor.rgb - c2); float m2 = max(max(d2.r, d2.g), d2.b); float w2 = step(m2, threshold) * (1.0 - m2 / (threshold + 0.0001)); accumColor += c2 * w2; weightSum += w2;\n" +
            "    half3 d3 = abs(origColor.rgb - c3); float m3 = max(max(d3.r, d3.g), d3.b); float w3 = step(m3, threshold) * (1.0 - m3 / (threshold + 0.0001)); accumColor += c3 * w3; weightSum += w3;\n" +
            "    half3 d4 = abs(origColor.rgb - c4); float m4 = max(max(d4.r, d4.g), d4.b); float w4 = step(m4, threshold) * (1.0 - m4 / (threshold + 0.0001)); accumColor += c4 * w4; weightSum += w4;\n" +
            "    half3 d5 = abs(origColor.rgb - c5); float m5 = max(max(d5.r, d5.g), d5.b); float w5 = step(m5, threshold) * (1.0 - m5 / (threshold + 0.0001)); accumColor += c5 * w5; weightSum += w5;\n" +
            "    half3 d6 = abs(origColor.rgb - c6); float m6 = max(max(d6.r, d6.g), d6.b); float w6 = step(m6, threshold) * (1.0 - m6 / (threshold + 0.0001)); accumColor += c6 * w6; weightSum += w6;\n" +
            "    half3 d7 = abs(origColor.rgb - c7); float m7 = max(max(d7.r, d7.g), d7.b); float w7 = step(m7, threshold) * (1.0 - m7 / (threshold + 0.0001)); accumColor += c7 * w7; weightSum += w7;\n" +
            "    half3 smoothedColor = accumColor / weightSum;\n" +
            "    float n1 = rand(fragCoord + float2(1.7, 3.1));\n" +
            "    float n2 = rand(fragCoord + float2(2.3, 4.2));\n" +
            "    float triDither = (n1 + n2 - 1.0) * (0.015 * in_debandStrength);\n" +
            "    half3 finalRgb = clamp(smoothedColor + float3(triDither), 0.0, 1.0);\n" +
            "    return half4(finalRgb, origColor.a);\n" +
            "}\n";

    private final Context context;
    private final PlayerView playerView;
    private final DebandingOverlayView debandingOverlayView;

    private boolean isEnabled = false;
    private float strength = 1.0f;

    public DebandingManager(@NonNull Context context,
                            @Nullable PlayerView playerView,
                            @Nullable DebandingOverlayView debandingOverlayView) {
        this.context = context;
        this.playerView = playerView;
        this.debandingOverlayView = debandingOverlayView;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        applyDebandingState();
        Log.d(TAG, "Debanding " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setStrength(float strength) {
        this.strength = Math.max(0.0f, Math.min(1.0f, strength));
        if (isEnabled) {
            applyDebandingState();
        }
    }

    public float getStrength() {
        return strength;
    }

    private void applyDebandingState() {
        boolean active = isEnabled && strength > 0.001f;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // AGSL is supported and highly optimized (1 texture fetch, minimal GPU load)
            if (debandingOverlayView != null) {
                debandingOverlayView.setVisibility(View.GONE);
            }

            if (playerView != null) {
                try {
                    if (active) {
                        RuntimeShader shader = new RuntimeShader(AGSL_DEBAND_SHADER);
                        shader.setFloatUniform("in_debandStrength", strength);
                        RenderEffect effect = RenderEffect.createRuntimeShaderEffect(shader, "composer");
                        playerView.setRenderEffect(effect);
                    } else {
                        playerView.setRenderEffect(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to apply AGSL Deband RenderEffect", e);
                    if (debandingOverlayView != null) {
                        debandingOverlayView.setVisibility(active ? View.VISIBLE : View.GONE);
                        debandingOverlayView.setIntensity(active ? strength : 0.0f);
                    }
                }
            }
        } else {
            // Fallback for older Android versions
            if (playerView != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    playerView.setRenderEffect(null);
                }
            }
            if (debandingOverlayView != null) {
                debandingOverlayView.setVisibility(active ? View.VISIBLE : View.GONE);
                debandingOverlayView.setIntensity(active ? strength : 0.0f);
            }
        }
    }
}
