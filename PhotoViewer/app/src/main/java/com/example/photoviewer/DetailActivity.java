package com.example.photoviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 게시물 상세 화면.
 * Bitmap을 Intent로 직접 전달하지 않고, title/text/date/imageUrl 문자열만 받아서
 * 이미지는 이 화면에서 imageUrl로 직접 다시 내려받는다.
 */
public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_FAVORITE = "extra_favorite";

    ZoomableImageView detailImage;
    TextView noImageLabel;
    TextView titleView;
    TextView dateView;
    TextView textView;
    TextView favoriteIconView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        detailImage = findViewById(R.id.detailImage);
        noImageLabel = findViewById(R.id.noImageLabel);
        titleView = findViewById(R.id.detailTitle);
        dateView = findViewById(R.id.detailDate);
        textView = findViewById(R.id.detailText);
        favoriteIconView = findViewById(R.id.detailFavoriteIcon);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String text = getIntent().getStringExtra(EXTRA_TEXT);
        String date = getIntent().getStringExtra(EXTRA_DATE);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        boolean favorite = getIntent().getBooleanExtra(EXTRA_FAVORITE, false);

        titleView.setText(title);
        dateView.setText(date);
        textView.setText(text);
        favoriteIconView.setText(favorite ? "♥" : "♡");

        // org.json은 JSON null 값을 optString()으로 읽으면 문자열 "null"을 반환하므로
        // null/빈 문자열뿐 아니라 "null" 문자열도 "이미지 없음"으로 취급해야 한다.
        boolean hasImage = !TextUtils.isEmpty(imageUrl) && !"null".equals(imageUrl);

        if (!hasImage) {
            // 이미지가 없는 게시물: 다운로드를 시도하지 않고 안내만 표시한다 (크래시 방지).
            detailImage.setVisibility(View.GONE);
            noImageLabel.setText("이미지 없음");
            noImageLabel.setVisibility(View.VISIBLE);
        } else {
            new LoadImageTask().execute(imageUrl);
        }
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                return bitmap;
            } catch (IOException e) {
                Log.e(TAG, "상세 이미지 다운로드 실패", e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                detailImage.setVisibility(View.VISIBLE);
                detailImage.setImageBitmap(bitmap);
                detailImage.resetZoom();
                noImageLabel.setVisibility(View.GONE);
            } else {
                detailImage.setVisibility(View.GONE);
                noImageLabel.setText("이미지를 불러올 수 없습니다.");
                noImageLabel.setVisibility(View.VISIBLE);
            }
        }
    }
}
