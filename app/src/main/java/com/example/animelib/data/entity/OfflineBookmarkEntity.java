package com.example.animelib.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_bookmarks")
public class OfflineBookmarkEntity {
    @PrimaryKey
    @NonNull
    private String animeId;
    private int episodeId;
    private String episodeNumber;
    private String timecode;
    private long positionMs;
    private long updatedAt;

    public OfflineBookmarkEntity(@NonNull String animeId, int episodeId, String episodeNumber,
                                 String timecode, long positionMs, long updatedAt) {
        this.animeId = animeId;
        this.episodeId = episodeId;
        this.episodeNumber = episodeNumber;
        this.timecode = timecode;
        this.positionMs = positionMs;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getAnimeId() { return animeId; }
    public void setAnimeId(@NonNull String animeId) { this.animeId = animeId; }

    public int getEpisodeId() { return episodeId; }
    public void setEpisodeId(int episodeId) { this.episodeId = episodeId; }

    public String getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(String episodeNumber) { this.episodeNumber = episodeNumber; }

    public String getTimecode() { return timecode; }
    public void setTimecode(String timecode) { this.timecode = timecode; }

    public long getPositionMs() { return positionMs; }
    public void setPositionMs(long positionMs) { this.positionMs = positionMs; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
