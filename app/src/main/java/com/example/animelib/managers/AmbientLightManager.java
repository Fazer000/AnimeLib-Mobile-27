package com.example.animelib.managers;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.AmbientVignetteOverlayView;
import com.example.animelib.util.MediaCacheManager;

/**
 * Менеджер фоновой подсветки (Ambilight) на основе второго легковесного ExoPlayer,
 * расположенного непосредственно под основным видеоплеером.
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLightManager";
    private static final long SYNC_THRESHOLD_MS = 300; // Допустимый рассинхрон перед подтяжкой кадра

    private final Context context;
    private final PlayerView mainPlayerView;
    private final View ambientContainer;
    private final PlayerView ambientPlayerView;
    private final AmbientVignetteOverlayView ambientVignetteOverlay;
    private final Handler mainHandler;

    private ExoPlayer mainPlayer;
    private ExoPlayer ambientPlayer;

    private DataSource.Factory cacheDataSourceFactory;
    private MediaItem currentMediaItem;
    private String currentVideoUrl;

    private volatile boolean isEnabled = false;
    private volatile boolean isPrepared = false;
    private boolean isErrorState = false;
    private boolean isSuspended = false;
    private boolean isFrozen = false;

    private Player.Listener mainPlayerListener;
    private Player.Listener ambientPlayerListener;

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncPositionIfNeeded();
            if (isEnabled && !isSuspended && !isFrozen && mainPlayer != null && mainPlayer.isPlaying() && !isErrorState) {
                mainHandler.postDelayed(this, 1000);
            }
        }
    };

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable PlayerView ambientPlayerView) {
        this(context, mainPlayerView, null, ambientPlayerView, null);
    }

    public AmbientLightManager(@NonNull Context context,
                               @NonNull PlayerView mainPlayerView,
                               @Nullable View ambientContainer,
                               @Nullable PlayerView ambientPlayerView,
                               @Nullable AmbientVignetteOverlayView ambientVignetteOverlay) {
        this.context = context;
        this.mainPlayerView = mainPlayerView;
        this.ambientContainer = ambientContainer;
        this.ambientPlayerView = ambientPlayerView;
        this.ambientVignetteOverlay = ambientVignetteOverlay;
        this.mainHandler = new Handler(Looper.getMainLooper());

        setupAmbientViewStyle();
    }

    /**
     * Первичная настройка второго плеера (растяжение fill, масштаб и блюр)
     */
    private void setupAmbientViewStyle() {
        if (ambientContainer != null) {
            ambientContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        if (ambientPlayerView == null) return;

        ambientPlayerView.setUseController(false);
        ambientPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        ambientPlayerView.setScaleX(1.01f);
        ambientPlayerView.setScaleY(1.01f);
        ambientPlayerView.setAlpha(1.0f);

        // Сильный аппаратный блюр GPU + увеличение насыщенности и яркости свечения (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                RenderEffect blurEffect = RenderEffect.createBlurEffect(150f, 150f, Shader.TileMode.CLAMP);
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(1.4f); // Насыщенность +40%
                ColorMatrix scaleMatrix = new ColorMatrix();
                scaleMatrix.setScale(1.2f, 1.2f, 1.2f, 1.0f); // Яркость +20%
                colorMatrix.postConcat(scaleMatrix);

                RenderEffect colorEffect = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(colorMatrix));
                RenderEffect combinedEffect = RenderEffect.createChainEffect(blurEffect, colorEffect);
                ambientPlayerView.setRenderEffect(combinedEffect);
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply RenderEffect blur", e);
            }
        } else {
            ambientPlayerView.setAlpha(0.95f);
        }
    }

    public void setDataSourceFactory(DataSource.Factory dataSourceFactory) {
        if (dataSourceFactory != null) {
            this.cacheDataSourceFactory = MediaCacheManager.createCacheDataSourceFactory(context, dataSourceFactory);
        }
    }

    public void setPlayer(@Nullable ExoPlayer mainPlayer) {
        setPlayer(mainPlayer, null, null);
    }

    public void setPlayer(@Nullable ExoPlayer mainPlayer, @Nullable MediaItem mediaItem, @Nullable String videoUrl) {
        if (this.mainPlayer != null && mainPlayerListener != null) {
            this.mainPlayer.removeListener(mainPlayerListener);
        }

        this.mainPlayer = mainPlayer;
        if (mediaItem != null) this.currentMediaItem = mediaItem;
        if (videoUrl != null) this.currentVideoUrl = videoUrl;

        if (mainPlayer == null) {
            releaseAmbientPlayer();
            return;
        }

        setupMainPlayerListener();

        if (isEnabled && !isSuspended) {
            ensureAmbientPlayerInitialized();
        }
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;

        mainHandler.post(() -> {
            boolean visible = enabled && !isErrorState && !isSuspended;
            if (ambientContainer != null) {
                ambientContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            } else if (ambientPlayerView != null) {
                ambientPlayerView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            if (enabled && !isSuspended) {
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            } else {
                pauseAmbientPlayer();
            }
        });

        Log.d(TAG, "Ambient light " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void suspend() {
        this.isSuspended = true;
        mainHandler.post(() -> {
            if (ambientContainer != null) {
                ambientContainer.setVisibility(View.GONE);
            } else if (ambientPlayerView != null) {
                ambientPlayerView.setVisibility(View.GONE);
            }
            pauseAmbientPlayer();
        });
    }

    public void freeze() {
        this.isFrozen = true;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
            }
            pauseAmbientPlayer();
        });
    }

    public void unfreeze() {
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState && !isSuspended) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            }
        });
    }

    public void resume() {
        this.isSuspended = false;
        this.isFrozen = false;
        mainHandler.post(() -> {
            if (isEnabled && !isErrorState) {
                if (ambientContainer != null) {
                    ambientContainer.setVisibility(View.VISIBLE);
                } else if (ambientPlayerView != null) {
                    ambientPlayerView.setVisibility(View.VISIBLE);
                }
                ensureAmbientPlayerInitialized();
                syncWithMainPlayerState();
            }
        });
    }

    /**
     * Инициализация второго плеера с ультра-низким энергопотреблением
     */
    private void ensureAmbientPlayerInitialized() {
        if (!isEnabled || mainPlayer == null || isErrorState || isSuspended || ambientPlayerView == null) return;

        if (ambientPlayer == null) {
            try {
                ExoPlayer.Builder builder = new ExoPlayer.Builder(context);
                if (cacheDataSourceFactory != null) {
                    builder.setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory));
                }

                ambientPlayer = builder.build();
                ambientPlayerView.setPlayer(ambientPlayer);

                // ОПТИМИЗАЦИЯ ДЛЯ МИНИМАЛЬНОГО ПОТРЕБЛЕНИЯ РЕСУРСОВ:
                // 1. Отключаем звук полностью
                ambientPlayer.setVolume(0f);

                // 2. Отключаем аудио и текстовые треки, ограничиваем максимальное качество до 240p/360p
                TrackSelectionParameters parameters = ambientPlayer.getTrackSelectionParameters()
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .setMaxVideoSize(360, 240)
                        .setMaxVideoBitrate(350_000)
                        .build();
                ambientPlayer.setTrackSelectionParameters(parameters);

                setupAmbientPlayerListener();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create ambient ExoPlayer", e);
                isErrorState = true;
                if (ambientPlayerView != null) ambientPlayerView.setVisibility(View.GONE);
                return;
            }
        }

        prepareAmbientMedia();
    }

    private void prepareAmbientMedia() {
        if (ambientPlayer == null || mainPlayer == null || isErrorState) return;

        try {
            MediaItem mediaItemToUse = currentMediaItem;
            if (mediaItemToUse == null && currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
                mediaItemToUse = MediaItem.fromUri(currentVideoUrl);
            }

            if (mediaItemToUse != null) {
                ambientPlayer.setMediaItem(mediaItemToUse);
                ambientPlayer.prepare();
                ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
                if (mainPlayer.isPlaying()) {
                    ambientPlayer.play();
                } else {
                    ambientPlayer.pause();
                }
                isPrepared = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing ambient media", e);
        }
    }

    private void setupMainPlayerListener() {
        if (mainPlayer == null) return;

        mainPlayerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null || isErrorState) return;
                if (isPlaying) {
                    syncPositionIfNeeded();
                    ambientPlayer.play();
                    mainHandler.removeCallbacks(syncRunnable);
                    mainHandler.post(syncRunnable);
                } else {
                    ambientPlayer.pause();
                    mainHandler.removeCallbacks(syncRunnable);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null || isErrorState) return;
                if (playbackState == Player.STATE_READY) {
                    if (!isPrepared) {
                        prepareAmbientMedia();
                    } else {
                        syncPositionIfNeeded();
                    }
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null || isErrorState) return;
                ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                if (!isEnabled || isSuspended || isFrozen || ambientPlayer == null || isErrorState) return;
                ambientPlayer.setPlaybackParameters(playbackParameters);
            }
        };

        mainPlayer.addListener(mainPlayerListener);
    }

    private void setupAmbientPlayerListener() {
        if (ambientPlayer == null) return;

        ambientPlayerListener = new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.w(TAG, "Ambient player encountered playback error: " + error.getMessage() + ". Disabling ambient player safely.");
                isErrorState = true;
                mainHandler.post(() -> {
                    if (ambientPlayerView != null) ambientPlayerView.setVisibility(View.GONE);
                    pauseAmbientPlayer();
                });
            }
        };

        ambientPlayer.addListener(ambientPlayerListener);
    }

    private void syncPositionIfNeeded() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            long mainPos = mainPlayer.getCurrentPosition();
            long ambientPos = ambientPlayer.getCurrentPosition();
            if (Math.abs(mainPos - ambientPos) > SYNC_THRESHOLD_MS) {
                ambientPlayer.seekTo(mainPos);
            }
        } catch (Exception ignored) {}
    }

    private void syncWithMainPlayerState() {
        if (!isEnabled || isSuspended || isFrozen || mainPlayer == null || ambientPlayer == null || isErrorState) return;
        try {
            ambientPlayer.setPlaybackParameters(mainPlayer.getPlaybackParameters());
            ambientPlayer.seekTo(mainPlayer.getCurrentPosition());
            if (mainPlayer.isPlaying()) {
                ambientPlayer.play();
                mainHandler.removeCallbacks(syncRunnable);
                mainHandler.post(syncRunnable);
            } else {
                ambientPlayer.pause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing with main player state", e);
        }
    }

    private void pauseAmbientPlayer() {
        if (ambientPlayer != null) {
            try {
                ambientPlayer.pause();
            } catch (Exception ignored) {}
        }
        mainHandler.removeCallbacks(syncRunnable);
    }

    public void releaseAmbientPlayer() {
        mainHandler.removeCallbacks(syncRunnable);
        if (mainPlayer != null && mainPlayerListener != null) {
            mainPlayer.removeListener(mainPlayerListener);
            mainPlayerListener = null;
        }

        if (ambientPlayer != null) {
            try {
                if (ambientPlayerListener != null) {
                    ambientPlayer.removeListener(ambientPlayerListener);
                    ambientPlayerListener = null;
                }
                ambientPlayer.stop();
                ambientPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing ambientPlayer", e);
            }
            ambientPlayer = null;
        }
        isPrepared = false;
        isErrorState = false;
    }

    public void cleanup() {
        releaseAmbientPlayer();
    }

    public void onPause() {
        suspend();
    }

    public void onResume() {
        resume();
    }

    public void onDestroy() {
        cleanup();
    }
}
