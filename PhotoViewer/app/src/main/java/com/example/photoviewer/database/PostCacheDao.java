package com.example.photoviewer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface PostCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PostCacheEntity> posts);

    @Query("DELETE FROM post_cache")
    void clearAll();

    @Query("SELECT * FROM post_cache")
    List<PostCacheEntity> getAll();

    /** 온라인 동기화가 성공할 때마다 캐시를 서버 최신 데이터로 통째로 교체한다. */
    @Transaction
    default void replaceAll(List<PostCacheEntity> posts) {
        clearAll();
        insertAll(posts);
    }
}
