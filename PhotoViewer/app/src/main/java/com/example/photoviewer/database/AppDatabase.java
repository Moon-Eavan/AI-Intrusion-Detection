package com.example.photoviewer.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * version 1 -> 2: PostCacheEntity(post_cache 테이블)를 새로 추가했다.
 *
 * 이번에는 Phase 11.5 때와 달리 fallbackToDestructiveMigration()을 쓰지 않는다.
 * 그 방식은 DB 전체(기존 favorites 테이블 포함)를 삭제하고 새로 만들기 때문에,
 * 이미 등록된 즐겨찾기 데이터까지 함께 사라진다. 지금은 테이블을 "추가"하는 것뿐이고
 * 기존 favorites 테이블의 컬럼/데이터는 전혀 바뀌지 않으므로, MIGRATION_1_2에서
 * post_cache 테이블만 새로 만드는 명시적 마이그레이션을 사용해 즐겨찾기 데이터를 보존한다.
 *
 * exportSchema=false라 Room이 생성한 schema json은 없지만, CREATE TABLE 문은
 * PostCacheEntity에 선언한 필드(전부 TEXT, postKey가 PRIMARY KEY)를 그대로 반영했다.
 */
@Database(entities = {FavoriteEntity.class, PostCacheEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract FavoriteDao favoriteDao();

    public abstract PostCacheDao postCacheDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `post_cache` ("
                    + "`postKey` TEXT NOT NULL, "
                    + "`author` TEXT, "
                    + "`title` TEXT, "
                    + "`text` TEXT, "
                    + "`createdDate` TEXT, "
                    + "`publishedDate` TEXT, "
                    + "`imageUrl` TEXT, "
                    + "PRIMARY KEY(`postKey`))");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "photoviewer.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
