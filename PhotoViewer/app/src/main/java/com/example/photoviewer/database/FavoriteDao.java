package com.example.photoviewer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity entity);

    @Query("DELETE FROM favorites WHERE postKey = :postKey")
    void deleteByKey(String postKey);

    @Query("SELECT postKey FROM favorites")
    List<String> getAllKeys();
}
