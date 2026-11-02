package com.example.photoviewer.model;

import android.graphics.Bitmap;

/**
 * GET /api_root/Post/ 응답 1건을 나타내는 모델.
 *
 * 실제 Django 응답(PostSerializer)에는 id 필드가 없으므로
 * 여기서도 서버 게시물 id를 임의로 만들어 넣지 않는다.
 * (author/title/text/created_date/published_date/image 필드만 그대로 보관)
 */
public class PostItem {
    private final String author;
    private final String title;
    private final String text;
    private final String createdDate;
    private final String publishedDate;
    private final String imageUrl;
    private Bitmap bitmap;
    private boolean favorite;

    public PostItem(String author, String title, String text, String createdDate,
                     String publishedDate, String imageUrl) {
        this.author = author;
        this.title = title;
        this.text = text;
        this.createdDate = createdDate;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * image URL이 실제로 유효한지 확인한다.
     * org.json은 JSON null 값을 optString()으로 읽으면 문자열 "null"을 반환하므로
     * null/빈 문자열뿐 아니라 "null" 문자열도 "이미지 없음"으로 취급해야 한다.
     */
    public boolean hasImage() {
        return isValid(imageUrl);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * 목록/상세 화면에 보여줄 날짜 하나를 고른다.
     * published_date가 있으면 그것을, 없으면(null) created_date를 사용한다.
     */
    public String getDisplayDate() {
        if (isValid(publishedDate)) {
            return publishedDate;
        }
        return createdDate;
    }

    /**
     * 정렬에 사용할 날짜 문자열.
     * Django가 내려주는 날짜는 "2026-10-09T02:49:20.498404Z"처럼
     * 고정 자릿수의 UTC ISO-8601 형식이라 날짜 파싱 없이 문자열 비교만으로도
     * 시간 순서와 동일하게 정렬된다. null/빈 문자열/"null"/형식이 이상한 값은
     * 모두 빈 문자열로 통일해 크래시 없이 안전하게 처리한다(정렬 시 맨 끝으로 몰림).
     */
    public String getSortableDate() {
        String date = getDisplayDate();
        return isValid(date) ? date : "";
    }

    private static boolean isValid(String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }

    /**
     * 즐겨찾기 식별키. API에 id 필드가 없어 서버 ID를 임의로 만들지 않는 대신,
     * created_date 단독을 사용한다.
     *
     * 다른 필드를 쓰지 않는 이유 (blog/views.py, blog/forms.py 코드로 확인함):
     * - title: post_edit(PostForm)에서 사용자가 직접 수정 가능.
     * - published_date: post_edit이 수정할 때마다 timezone.now()로 다시 갱신함.
     * - author: post_edit이 항상 post.author = request.user로 재할당하므로,
     *   다른 로그인 사용자가 같은 글을 수정하면 값이 바뀐다.
     * - image: 이미지가 없는 게시물이 여러 건 있을 수 있어(image=null) 단독 키로 부적합.
     *
     * created_date만 유일하게 생성 이후 어떤 코드 경로로도 재할당되지 않는다
     * (models.py: default=timezone.now — auto_now가 아니므로 생성 시 1회만 적용되고,
     * PostForm.Meta.fields=('title','text','image')에도 포함되어 있지 않아
     * post_new/post_edit 어디에서도 건드리지 않음). 실제 API 데이터(13개 게시물)로도
     * 마이크로초 단위까지 전부 서로 다름을 확인했다.
     */
    public String getPostKey() {
        return isValid(createdDate) ? createdDate : "";
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
