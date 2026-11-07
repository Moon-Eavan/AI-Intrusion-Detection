package com.example.photoviewer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ID 10 기능 A(원격 감시 설정): /api_root/config/ 호출 공용 코드.
 * MainActivity(Edge 상태 표시)와 SettingsActivity(설정 화면)가 함께 사용한다.
 * 네트워크 작업이므로 반드시 백그라운드 스레드(AsyncTask.doInBackground)에서 호출한다.
 */
public final class MonitorConfigApi {

    private static final String CONFIG_URL = ApiConfig.BASE_URL + "/api_root/config/";

    private MonitorConfigApi() {
    }

    /** 실패 시 서버 응답 본문(검증 오류 등)을 메시지로 담아 던진다. */
    public static class ApiException extends Exception {
        ApiException(String message) {
            super(message);
        }
    }

    public static JSONObject fetch() throws ApiException {
        return request("GET", null);
    }

    /** HttpURLConnection은 PATCH를 지원하지 않으므로 전체 설정 필드를 PUT으로 보낸다. */
    public static JSONObject save(JSONObject body) throws ApiException {
        return request("PUT", body);
    }

    private static JSONObject request(String method, JSONObject body) throws ApiException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(CONFIG_URL).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new JSONObject(readAll(conn.getInputStream()));
            }
            // 404: 설정 API가 없는 서버(기능 A 배포 전), 400: 값 검증 실패
            String error = conn.getErrorStream() != null ? readAll(conn.getErrorStream()) : "";
            throw new ApiException("HTTP " + responseCode + (responseCode == 400 ? " " + error : ""));
        } catch (IOException | JSONException e) {
            throw new ApiException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        try (InputStream in = is) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    /** "Edge 온라인 · 3초 전" / "Edge 오프라인 · 2분 전" / "Edge 접속 기록 없음" */
    public static String describeEdge(JSONObject config) {
        if (config.isNull("edge_seen_sec")) {
            return "Edge 접속 기록 없음";
        }
        long sec = config.optLong("edge_seen_sec");
        String ago;
        if (sec < 60) {
            ago = sec + "초 전";
        } else if (sec < 3600) {
            ago = (sec / 60) + "분 전";
        } else if (sec < 86400) {
            ago = (sec / 3600) + "시간 전";
        } else {
            ago = (sec / 86400) + "일 전";
        }
        return (config.optBoolean("edge_online") ? "Edge 온라인 · " : "Edge 오프라인 · ") + ago;
    }
}
