package com.example.photoviewer;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.photoviewer.database.AppDatabase;
import com.example.photoviewer.database.FavoriteEntity;
import com.example.photoviewer.database.PostCacheEntity;
import com.example.photoviewer.model.PostItem;
import com.example.photoviewer.worker.PostSyncWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private enum SortOrder { NONE, NEWEST, OLDEST }

    // PeriodicWorkRequest를 등록할 때 사용하는 고유 작업 이름 (enqueueUniquePeriodicWork용).
    private static final String SYNC_WORK_NAME = "post_sync_work";

    TextView textView;
    View offlineBanner;
    EditText searchInput;
    ImageAdapter adapter;

    FetchPostsTask taskDownload;

    // 원본 목록(서버에서 받아온 전체)과 화면 표시 목록(검색/정렬/즐겨찾기 필터 적용 결과)을 분리한다.
    private final List<PostItem> allPosts = new ArrayList<>();
    private List<PostItem> displayedPosts = new ArrayList<>();
    private SortOrder currentSortOrder = SortOrder.NONE;
    private boolean showFavoritesOnly = false;
    // 현재 화면이 서버 최신 데이터가 아니라 Room 오프라인 캐시를 보여주고 있는지 여부.
    private boolean isOfflineMode = false;

    private AppDatabase database;
    // Room에서 읽어온 즐겨찾기 postKey 모음 (메모리 캐시). 동기화로 PostItem이 새로 만들어질 때마다
    // 여기 들어있는 키와 비교해서 favorite 상태를 다시 적용한다.
    private final Set<String> favoriteKeys = new HashSet<>();

    private final ActivityResultLauncher<Intent> uploadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 업로드 성공 후 복귀 시 자동으로 재동기화하여 새 게시물을 바로 확인할 수 있게 한다.
                    startSync();
                }
            });

    // 거부되어도 앱은 그대로 동작한다. PostSyncWorker가 알림을 띄우기 직전에 권한을 다시 확인해서
    // 권한이 없으면 알림만 생략하고 동기화(baseline/중복 방지 갱신)는 계속 정상 수행한다.
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.textView);
        offlineBanner = findViewById(R.id.offlineBanner);
        searchInput = findViewById(R.id.searchInput);
        database = AppDatabase.getInstance(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new ImageAdapter(displayedPosts, this::toggleFavorite);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 앱을 다시 실행했을 때도 즐겨찾기가 유지되도록 Room에서 즐겨찾기 키 목록을 먼저 불러온다.
        new LoadFavoritesTask().execute();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilterAndSort();
            }
        });

        Button btnSortNewest = findViewById(R.id.btnSortNewest);
        Button btnSortOldest = findViewById(R.id.btnSortOldest);
        btnSortNewest.setOnClickListener(v -> {
            currentSortOrder = SortOrder.NEWEST;
            applyFilterAndSort();
        });
        btnSortOldest.setOnClickListener(v -> {
            currentSortOrder = SortOrder.OLDEST;
            applyFilterAndSort();
        });

        Button btnFilterAll = findViewById(R.id.btnFilterAll);
        Button btnFilterFavorites = findViewById(R.id.btnFilterFavorites);
        btnFilterAll.setOnClickListener(v -> {
            showFavoritesOnly = false;
            applyFilterAndSort();
        });
        btnFilterFavorites.setOnClickListener(v -> {
            showFavoritesOnly = true;
            applyFilterAndSort();
        });

        requestNotificationPermissionIfNeeded();
        schedulePeriodicSync();
    }

    /** Android 13(API 33)+에서는 Notification을 띄우려면 런타임 권한이 필요하다. */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * 앱이 실행 중이 아니어도 주기적으로 새 게시물을 확인하도록 PostSyncWorker를 등록한다.
     * PeriodicWorkRequest는 시스템이 허용하는 최소 반복 주기(15분)보다 짧게 설정할 수 없으므로
     * 그보다 짧은 주기를 억지로 쓰지 않고 최소값인 15분을 그대로 사용한다.
     * ExistingPeriodicWorkPolicy.KEEP을 사용해, 이미 등록되어 있으면 앱을 다시 켤 때마다
     * 스케줄이 리셋되지 않고 기존 주기가 유지되게 한다.
     */
    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PostSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** 개발/보고서 검증용: PeriodicWorkRequest의 15분 주기를 기다리지 않고 PostSyncWorker를 즉시 1회 실행한다. */
    public void onClickTestBackgroundSync(View v) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PostSyncWorker.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(request);
        Toast.makeText(getApplicationContext(), "백그라운드 동기화를 즉시 실행했습니다.", Toast.LENGTH_LONG).show();
    }

    /**
     * allPosts(원본 목록)에 현재 검색어/정렬 상태를 적용해 displayedPosts(표시 목록)를 다시 계산하고
     * RecyclerView와 상태 TextView를 갱신한다. 서버를 다시 호출하지 않는다.
     */
    private void applyFilterAndSort() {
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());

        List<PostItem> filtered = new ArrayList<>();
        for (PostItem item : allPosts) {
            if (showFavoritesOnly && !item.isFavorite()) {
                continue;
            }
            if (matchesQuery(item, query)) {
                filtered.add(item);
            }
        }

        if (currentSortOrder == SortOrder.NEWEST) {
            Collections.sort(filtered, (a, b) -> b.getSortableDate().compareTo(a.getSortableDate()));
        } else if (currentSortOrder == SortOrder.OLDEST) {
            Collections.sort(filtered, (a, b) -> a.getSortableDate().compareTo(b.getSortableDate()));
        }
        // SortOrder.NONE이면 서버가 내려준 순서를 그대로 유지한다.

        displayedPosts = filtered;
        adapter.updateItems(displayedPosts);
        updateStatusText(query);
    }

    private boolean matchesQuery(PostItem item, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String title = item.getTitle() != null ? item.getTitle().toLowerCase(Locale.getDefault()) : "";
        String text = item.getText() != null ? item.getText().toLowerCase(Locale.getDefault()) : "";
        return title.contains(query) || text.contains(query);
    }

    private void updateStatusText(String query) {
        // UI 표시 전용: 오프라인 캐시 사용 여부(isOfflineMode)에 따라 경고 배너만 보이거나 숨긴다.
        offlineBanner.setVisibility(isOfflineMode ? View.VISIBLE : View.GONE);

        // 오프라인 캐시로 표시 중이면, 어떤 검색/정렬/필터 조합이어도 그 사실을 계속 알 수 있게 한다.
        String offlineSuffix = isOfflineMode ? " (오프라인 캐시)" : "";

        if (allPosts.isEmpty()) {
            textView.setText(isOfflineMode
                    ? "오프라인 캐시에도 저장된 게시물이 없습니다."
                    : "불러올 게시물이 없습니다.");
        } else if (displayedPosts.isEmpty()) {
            if (!query.isEmpty()) {
                textView.setText("검색 결과가 없습니다." + offlineSuffix);
            } else if (showFavoritesOnly) {
                textView.setText("즐겨찾기한 게시물이 없습니다." + offlineSuffix);
            } else {
                textView.setText("표시할 게시물이 없습니다." + offlineSuffix);
            }
        } else if (!query.isEmpty()) {
            textView.setText("검색 결과 " + displayedPosts.size() + "개" + offlineSuffix);
        } else if (showFavoritesOnly) {
            textView.setText("즐겨찾기 " + displayedPosts.size() + "개" + offlineSuffix);
        } else if (isOfflineMode) {
            textView.setText("오프라인 캐시에서 게시물 " + displayedPosts.size() + "개를 불러왔습니다.");
        } else {
            textView.setText("게시물 " + displayedPosts.size() + "개를 불러왔습니다.");
        }
    }

    /** allPosts의 각 PostItem에 Room에 저장된 즐겨찾기 상태를 다시 반영한다. */
    private void applyFavoriteStatus() {
        for (PostItem item : allPosts) {
            item.setFavorite(favoriteKeys.contains(item.getPostKey()));
        }
    }

    /**
     * RecyclerView의 하트 아이콘을 눌렀을 때 호출된다.
     * 메모리 상태(즉시 반영)와 Room DB(백그라운드 반영)를 함께 갱신한다.
     */
    private void toggleFavorite(PostItem postItem) {
        boolean newFavorite = !postItem.isFavorite();
        postItem.setFavorite(newFavorite);

        String postKey = postItem.getPostKey();
        if (newFavorite) {
            favoriteKeys.add(postKey);
        } else {
            favoriteKeys.remove(postKey);
        }

        new ToggleFavoriteTask(postKey, postItem.getTitle(), postItem.getImageUrl(), newFavorite).execute();

        // 즐겨찾기만 보기 필터가 켜져 있으면 해제 시 목록에서 바로 사라져야 하므로 다시 계산한다.
        applyFilterAndSort();
    }

    private class LoadFavoritesTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            return database.favoriteDao().getAllKeys();
        }

        @Override
        protected void onPostExecute(List<String> keys) {
            favoriteKeys.clear();
            if (keys != null) {
                favoriteKeys.addAll(keys);
            }
            applyFavoriteStatus();
            applyFilterAndSort();
        }
    }

    private class ToggleFavoriteTask extends AsyncTask<Void, Void, Void> {
        private final String postKey;
        private final String title;
        private final String imageUrl;
        private final boolean favorite;

        ToggleFavoriteTask(String postKey, String title, String imageUrl, boolean favorite) {
            this.postKey = postKey;
            this.title = title;
            this.imageUrl = imageUrl;
            this.favorite = favorite;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (favorite) {
                database.favoriteDao().insert(new FavoriteEntity(postKey, title, imageUrl));
            } else {
                database.favoriteDao().deleteByKey(postKey);
            }
            return null;
        }
    }

    public void onClickDownload(View v) {
        startSync();
    }

    private void startSync() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        taskDownload = new FetchPostsTask();
        taskDownload.execute(ApiConfig.BASE_URL + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        uploadLauncher.launch(new Intent(this, UploadActivity.class));
    }

    private static class FetchResult {
        boolean success;
        List<PostItem> postItems;
        String errorMessage;
    }

    private class FetchPostsTask extends AsyncTask<String, Void, FetchResult> {
        @Override
        protected FetchResult doInBackground(String... urls) {
            FetchResult result = new FetchResult();
            HttpURLConnection conn = null;

            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder resultText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resultText.append(line);
                    }
                    is.close();

                    String strJson = resultText.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    List<PostItem> postItemList = new ArrayList<>();

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post = aryJson.getJSONObject(i);
                        String author = post.optString("author");
                        String title = post.optString("title");
                        String text = post.optString("text");
                        String createdDate = post.optString("created_date");
                        String publishedDate = post.optString("published_date");
                        String imageUrl = post.optString("image");

                        Log.d(TAG, "post[" + i + "] author=" + author
                                + ", title=" + title
                                + ", text=" + text
                                + ", created_date=" + createdDate
                                + ", published_date=" + publishedDate
                                + ", image=" + imageUrl);

                        PostItem postItem = new PostItem(author, title, text, createdDate, publishedDate, imageUrl);
                        // image URL이 없거나 다운로드/디코딩에 실패해도 Bitmap만 null로 두고
                        // 게시물 자체는 목록에서 제외하지 않는다 (제목/본문/날짜/상세화면은 정상 표시되어야 함).
                        postItemList.add(postItem);

                        // 게시물 1건의 이미지 다운로드가 실패해도 나머지 목록 처리는 계속한다.
                        if (postItem.hasImage()) {
                            HttpURLConnection imgConn = null;
                            try {
                                URL myImageUrl = new URL(imageUrl);
                                imgConn = (HttpURLConnection) myImageUrl.openConnection();
                                imgConn.setConnectTimeout(5000);
                                imgConn.setReadTimeout(5000);

                                InputStream imgStream = imgConn.getInputStream();
                                Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                                imgStream.close();

                                if (imageBitmap != null) {
                                    postItem.setBitmap(imageBitmap);
                                } else {
                                    Log.e(TAG, "이미지 디코딩 실패: " + imageUrl);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "이미지 다운로드 실패: " + imageUrl, e);
                            } finally {
                                if (imgConn != null) {
                                    imgConn.disconnect();
                                }
                            }
                        }
                    }

                    result.success = true;
                    result.postItems = postItemList;
                } else {
                    result.success = false;
                    result.errorMessage = "HTTP " + responseCode;
                    Log.e(TAG, "GET 실패: HTTP " + responseCode);
                }
            } catch (IOException | JSONException e) {
                result.success = false;
                result.errorMessage = e.getMessage();
                Log.e(TAG, "게시물 목록을 불러오는 중 오류 발생", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(FetchResult result) {
            if (result.success) {
                // 서버에서 새로 받은 결과로 원본 목록을 갱신하고,
                // 현재 검색어/정렬 상태를 유지한 채로 다시 적용한다 (Upload 후 자동 재동기화 포함).
                isOfflineMode = false;
                allPosts.clear();
                if (result.postItems != null) {
                    allPosts.addAll(result.postItems);
                }
                // 새로 받아온 PostItem은 매번 새 객체이므로, 저장된 즐겨찾기 키와 비교해 다시 표시해야 한다.
                applyFavoriteStatus();
                applyFilterAndSort();

                // 온라인 동기화 성공 시 Room 오프라인 캐시도 최신 데이터로 교체한다.
                new SaveCacheTask(new ArrayList<>(allPosts)).execute();
            } else {
                // 네트워크/서버 오류 시 Room에 저장된 마지막 게시물 목록으로 대체를 시도한다.
                new LoadCacheTask(result.errorMessage).execute();
            }
        }
    }

    private class SaveCacheTask extends AsyncTask<Void, Void, Void> {
        private final List<PostItem> items;

        SaveCacheTask(List<PostItem> items) {
            this.items = items;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<PostCacheEntity> entities = new ArrayList<>();
            for (PostItem item : items) {
                entities.add(new PostCacheEntity(item.getPostKey(), item.getAuthor(), item.getTitle(),
                        item.getText(), item.getCreatedDate(), item.getPublishedDate(), item.getImageUrl()));
            }
            database.postCacheDao().replaceAll(entities);
            return null;
        }
    }

    private class LoadCacheTask extends AsyncTask<Void, Void, List<PostCacheEntity>> {
        private final String errorMessage;

        LoadCacheTask(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        protected List<PostCacheEntity> doInBackground(Void... voids) {
            return database.postCacheDao().getAll();
        }

        @Override
        protected void onPostExecute(List<PostCacheEntity> cached) {
            if (cached != null && !cached.isEmpty()) {
                isOfflineMode = true;
                allPosts.clear();
                for (PostCacheEntity entity : cached) {
                    allPosts.add(new PostItem(entity.author, entity.title, entity.text,
                            entity.createdDate, entity.publishedDate, entity.imageUrl));
                }
                applyFavoriteStatus();
                applyFilterAndSort();
            } else {
                // 캐시도 없으면(최초 실행 등) 기존처럼 네트워크 오류 메시지만 표시한다.
                textView.setText("불러오기 실패: " + errorMessage);
            }
        }
    }
}
