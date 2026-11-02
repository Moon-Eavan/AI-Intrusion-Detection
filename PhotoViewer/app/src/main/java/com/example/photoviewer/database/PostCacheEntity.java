package com.example.photoviewer.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 마지막으로 성공한 GET /api_root/Post/ 응답을 그대로 저장해두는 오프라인 캐시 Entity.
 *
 * Bitmap(이미지 실제 데이터)은 저장하지 않는다 — imageUrl 문자열만 보관한다.
 * 목록 화면은 오프라인 상태에서 이미지 재다운로드를 시도하지 않고 "이미지 없음"으로
 * 안전하게 표시하며, 상세 화면에서 사용자가 열람할 때만 시도하되 실패해도
 * 기존 "이미지를 불러올 수 없습니다." 처리로 크래시 없이 넘어간다.
 */
@Entity(tableName = "post_cache")
public class PostCacheEntity {
    @PrimaryKey
    @NonNull
    public String postKey; // = created_date (FavoriteEntity와 동일한 식별키 체계)

    public String author;
    public String title;
    public String text;
    public String createdDate;
    public String publishedDate;
    public String imageUrl;

    public PostCacheEntity(@NonNull String postKey, String author, String title, String text,
                            String createdDate, String publishedDate, String imageUrl) {
        this.postKey = postKey;
        this.author = author;
        this.title = title;
        this.text = text;
        this.createdDate = createdDate;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
    }
}
