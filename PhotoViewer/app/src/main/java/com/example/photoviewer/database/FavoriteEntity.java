package com.example.photoviewer.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 즐겨찾기 1건을 나타내는 Room Entity.
 *
 * 현재 Django API 응답(PostSerializer)에는 id 필드가 없으므로, 서버 ID를 임의로
 * 만들어 쓰지 않는다. 대신 postKey(created_date 단독)를 식별키로 사용한다.
 * title/author/published_date는 post_edit에서 바뀔 수 있지만, created_date는
 * Django가 게시물 생성 시 1회만 설정하고 이후 어떤 코드 경로로도 재할당하지 않아
 * 유일하게 안정적이다 (PostItem.getPostKey() 참고).
 */
@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey
    @NonNull
    public String postKey;

    public String title;
    public String imageUrl;

    public FavoriteEntity(@NonNull String postKey, String title, String imageUrl) {
        this.postKey = postKey;
        this.title = title;
        this.imageUrl = imageUrl;
    }
}
