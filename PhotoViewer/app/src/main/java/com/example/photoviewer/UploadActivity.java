package com.example.photoviewer;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";

    EditText editTitle;
    EditText editText;
    ImageView imagePreview;
    ProgressBar progressBar;
    TextView statusText;
    Button btnPickImage;
    Button btnUpload;

    Uri selectedImageUri;
    UploadTask uploadTask;

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        editTitle = findViewById(R.id.editTitle);
        editText = findViewById(R.id.editText);
        imagePreview = findViewById(R.id.imagePreview);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnUpload = findViewById(R.id.btnUpload);
        Button btnCancel = findViewById(R.id.btnCancel);

        // targetSdk 36 기준 현재 권장 방식: Android Photo Picker (추가 저장소 권한 불필요)
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                imagePreview.setImageURI(uri);
            }
        });

        btnPickImage.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        btnUpload.setOnClickListener(v -> startUpload());
        btnCancel.setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void startUpload() {
        String title = editTitle.getText().toString().trim();
        String text = editText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(text)) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 이미지는 선택 입력이다 (서버 Post.image가 blank=True). 선택하지 않으면 selectedImageUri가 null로 전달되고,
        // UploadTask가 multipart 요청에서 image 파트만 생략한다. 제목/내용은 서버 필수 필드라 위에서 계속 검사한다.
        if (uploadTask != null && uploadTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("업로드 중...");
        setFormEnabled(false);

        uploadTask = new UploadTask(title, text, selectedImageUri);
        uploadTask.execute();
    }

    private void setFormEnabled(boolean enabled) {
        editTitle.setEnabled(enabled);
        editText.setEnabled(enabled);
        btnPickImage.setEnabled(enabled);
        btnUpload.setEnabled(enabled);
    }

    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
        private final String title;
        private final String text;
        private final Uri imageUri;
        private String errorMessage;

        UploadTask(String title, String text, Uri imageUri) {
            this.title = title;
            this.text = text;
            this.imageUri = imageUri;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            String boundary = "----PhotoViewerBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = null;

            try {
                // 이미지를 선택하지 않은 경우 imageBytes는 null이고 image 파트를 보내지 않는다.
                byte[] imageBytes = imageUri != null ? readImageBytes(imageUri) : null;
                String fileName = "upload_" + System.currentTimeMillis() + ".jpg";

                URL url = new URL(ApiConfig.BASE_URL + "/api_root/Post/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream os = conn.getOutputStream()) {
                    // author는 서버(BlogImages.perform_create)가 결정하므로 보내지 않는다.
                    writeFormField(os, boundary, "title", title);
                    writeFormField(os, boundary, "text", text);
                    if (imageBytes != null) {
                        writeFileField(os, boundary, "image", fileName, imageBytes);
                    }
                    os.write(("--" + boundary + "--\r\n").getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return true;
                } else {
                    errorMessage = "HTTP " + responseCode + " " + readErrorBody(conn);
                    Log.e(TAG, "업로드 실패: " + errorMessage);
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "업로드 중 오류 발생", e);
                errorMessage = e.getMessage();
                return false;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            setFormEnabled(true);

            if (success) {
                Toast.makeText(UploadActivity.this, "업로드 성공!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                statusText.setText("업로드 실패: " + errorMessage);
                Toast.makeText(UploadActivity.this, "업로드 실패: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }

        private byte[] readImageBytes(Uri uri) throws IOException {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                return bos.toByteArray();
            }
        }

        private String readErrorBody(HttpURLConnection conn) {
            try (InputStream is = conn.getErrorStream()) {
                if (is == null) {
                    return "";
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                return bos.toString("UTF-8");
            } catch (IOException e) {
                return "";
            }
        }

        private void writeFormField(OutputStream os, String boundary, String name, String value) throws IOException {
            String part = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                    + value + "\r\n";
            os.write(part.getBytes("UTF-8"));
        }

        private void writeFileField(OutputStream os, String boundary, String name, String fileName, byte[] data)
                throws IOException {
            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: image/jpeg\r\n\r\n";
            os.write(header.getBytes("UTF-8"));
            os.write(data);
            os.write("\r\n".getBytes("UTF-8"));
        }
    }
}
