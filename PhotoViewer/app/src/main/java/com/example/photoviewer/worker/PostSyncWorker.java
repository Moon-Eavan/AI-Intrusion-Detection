package com.example.photoviewer.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.photoviewer.ApiConfig;
import com.example.photoviewer.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 앱이 실행 중이 아니어도 WorkManager(PeriodicWorkRequest)가 주기적으로 실행해
 * 서버의 게시물 목록을 확인하고, 이전에 확인한 적 없는 새 게시물이 있으면 Notification을 띄운다.
 *
 * MainActivity/Room과는 독립적으로 동작한다 (SharedPreferences에 "알고 있는 게시물" 집합만 저장).
 * API 응답에는 id 필드가 없으므로, PostItem.getPostKey()와 동일하게 created_date를
 * 게시물 식별 키로 사용한다 (title/author/published_date는 수정될 수 있어 부적합).
 */
public class PostSyncWorker extends Worker {

    private static final String TAG = "PostSyncWorker";

    private static final String PREFS_NAME = "post_sync_prefs";
    private static final String KEY_KNOWN_POST_KEYS = "known_post_keys";
    private static final String KEY_BASELINE_DONE = "baseline_done";

    public static final String CHANNEL_ID = "post_sync_channel";
    private static final int NOTIFICATION_ID = 1001;

    public PostSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        Set<String> currentKeys;
        Map<String, String> titleByKey = new HashMap<>();
        HttpURLConnection conn = null;

        try {
            URL url = new URL(ApiConfig.BASE_URL + "/api_root/Post/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GET 실패: HTTP " + responseCode);
                return Result.retry();
            }

            StringBuilder resultText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    resultText.append(line);
                }
            }

            currentKeys = new HashSet<>();
            JSONArray array = new JSONArray(resultText.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject post = array.getJSONObject(i);
                String createdDate = post.optString("created_date");
                if (!isValid(createdDate)) {
                    // created_date가 없는 비정상 데이터는 식별할 수 없으므로 비교 대상에서 제외한다.
                    continue;
                }
                currentKeys.add(createdDate);
                titleByKey.put(createdDate, post.optString("title"));
            }
        } catch (IOException e) {
            // 서버가 꺼져있거나 네트워크가 끊긴 경우. 크래시/오류 알림 없이 다음 주기에 재시도한다.
            Log.w(TAG, "서버 접근 실패, 다음 주기에 재시도", e);
            return Result.retry();
        } catch (JSONException e) {
            Log.e(TAG, "서버 응답 파싱 실패", e);
            return Result.failure();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean baselineDone = prefs.getBoolean(KEY_BASELINE_DONE, false);

        if (!baselineDone) {
            // 최초 실행: 지금 서버에 있는 게시물 전체를 "이미 알고 있는 것"으로만 기록하고 알림은 생략한다.
            saveKnownKeys(prefs, currentKeys);
            return Result.success();
        }

        Set<String> knownKeys = new HashSet<>(prefs.getStringSet(KEY_KNOWN_POST_KEYS, new HashSet<>()));
        Set<String> newKeys = new HashSet<>(currentKeys);
        newKeys.removeAll(knownKeys);

        // currentKeys로 "교체"하면 안 된다 (예: db.sqlite3를 이전 스냅샷으로 되돌리는 등의 이유로
        // 특정 실행에서 일부 게시물이 응답에서 빠지면, knownKeys에서도 사라져 나중에 그 게시물이
        // 다시 나타날 때 "새 게시물"로 재판정되어 중복 알림이 발생한다).
        // 대신 기존 knownKeys ∪ 이번 currentKeys로 누적 저장해, 한 번 본 게시물은 응답에서
        // 일시적으로 빠지더라도 "이미 아는 것"으로 계속 기억한다.
        Set<String> updatedKnownKeys = new HashSet<>(knownKeys);
        updatedKnownKeys.addAll(currentKeys);
        saveKnownKeys(prefs, updatedKnownKeys);

        if (!newKeys.isEmpty()) {
            notifyNewPosts(context, newKeys, titleByKey);
        }

        return Result.success();
    }

    private void saveKnownKeys(SharedPreferences prefs, Set<String> keys) {
        prefs.edit()
                .putStringSet(KEY_KNOWN_POST_KEYS, keys)
                .putBoolean(KEY_BASELINE_DONE, true)
                .apply();
    }

    private void notifyNewPosts(Context context, Set<String> newKeys, Map<String, String> titleByKey) {
        ensureNotificationChannel(context);

        if (!hasNotificationPermission(context)) {
            // 알림 권한이 꺼져 있으면 조용히 넘어간다. baseline/중복 방지 로직은 이미 갱신되었으므로
            // 동기화 자체는 권한과 무관하게 계속 정상 동작한다.
            return;
        }

        String content;
        if (newKeys.size() == 1) {
            String postTitle = titleByKey.get(newKeys.iterator().next());
            content = isValid(postTitle)
                    ? "새로운 게시물이 등록되었습니다. \"" + postTitle + "\""
                    : "새로운 게시물이 등록되었습니다.";
        } else {
            content = "새 게시물 " + newKeys.size() + "개가 있습니다.";
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("PhotoViewer")
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // hasNotificationPermission() 확인 이후에도 레이스 컨디션 등으로 거부될 수 있어 방어적으로 처리한다.
            Log.w(TAG, "Notification 권한 없음, 알림 생략", e);
        }
    }

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** MainActivity를 거치지 않고 Worker가 먼저 실행되는 경우에도 안전하도록 Channel 생성을 멱등하게 보장한다. */
    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "게시물 자동 동기화", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("새 게시물이 등록되면 알려줍니다.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static boolean isValid(String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }
}
