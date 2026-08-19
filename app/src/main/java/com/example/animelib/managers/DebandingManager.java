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
            "half4 main(float2 fragCoord) {\n" +
            "    half4 color = composer.eval(fragCoord);\n" +
            "    if (in_debandStrength <= 0.001) return color;\n" +
            "    float2 step = float2(2.0, 2.0);\n" +
            "    half4 cUp    = composer.eval(fragCoord + float2(0.0, -step.y));\n" +
            "    half4 cDown  = composer.eval(fragCoord + float2(0.0, step.y));\n" +
            "    half4 cLeft  = composer.eval(fragCoord + float2(-step.x, 0.0));\n" +
            "    half4 cRight = composer.eval(fragCoord + float2(step.x, 0.0));\n" +
            "    half4 avgColor = (color + cUp + cDown + cLeft + cRight) * 0.2;\n" +
            "    half diff = max(max(abs(color.r - avgColor.r), abs(color.g - avgColor.g)), abs(color.b - avgColor.b));\n" +
            "    if (diff < 0.09) {\n" +
            "        float n = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);\n" +
            "        float dither = (n - 0.5) * 0.02 * in_debandStrength;\n" +
            "        half factor = clamp(1.0 - (diff / 0.09), 0.0, 1.0) * in_debandStrength;\n" +
            "        color.rgb = mix(color.rgb, avgColor.rgb, factor * 0.7) + float3(dither);\n" +
            "    }\n" +
            "    return color;\n" +
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

        if (debandingOverlayView != null) {
            debandingOverlayView.setVisibility(active ? View.VISIBLE : View.GONE);
            debandingOverlayView.setIntensity(active ? strength : 0.0f);
        }

        if (playerView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                }
            }
        }
    }
}
