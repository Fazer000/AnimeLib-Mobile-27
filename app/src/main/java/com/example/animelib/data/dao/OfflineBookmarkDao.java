package com.example.animelib.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.animelib.data.entity.OfflineBookmarkEntity;

@Dao
public interface OfflineBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveBookmark(OfflineBookmarkEntity bookmark);

    @Query("SELECT * FROM offline_bookmarks WHERE animeId = :animeId LIMIT 1")
    OfflineBookmarkEntity getBookmarkSync(String animeId);

    @Query("DELETE FROM offline_bookmarks WHERE animeId = :animeId")
    void deleteBookmark(String animeId);
}
