package com.example.photoviewer;

import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ID 10 기능 A: 원격 감시 설정 화면.
 * 서버(/api_root/config/)의 설정을 불러와 수정하고 저장하면, YOLO Edge가 polling(5초)으로 반영한다.
 */
public class SettingsActivity extends AppCompatActivity {

    // Slider는 신뢰도 임계값을 정수 %(25~90, 5 단위)로 다룬다
    private static final int CONF_MIN_PCT = 25;
    private static final int CONF_MAX_PCT = 90;
    private static final int CONF_STEP_PCT = 5;
    private static final long STATUS_REFRESH_MS = 5000;

    View edgeStatusDot;
    TextView edgeStatusText;
    MaterialSwitch switchEnabled;
    TextView labelConf;
    Slider sliderConf;
    EditText editCooldown;
    TextView labelTargets;
    TextView targetsHint;
    ChipGroup chipGroupTargets;
    TextView statusText;
    Button btnSave;

    private final Handler handler = new Handler(Looper.getMainLooper());
    // 폼 값은 처음 한 번만 채우고, 이후 주기적 갱신은 Edge 상태 표시만 바꾼다 (편집 중인 값 보호)
    private boolean formLoaded = false;

    private final Runnable statusRefresher = new Runnable() {
        @Override
        public void run() {
            new LoadConfigTask().execute();
            handler.postDelayed(this, STATUS_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edgeStatusDot = findViewById(R.id.edgeStatusDot);
        edgeStatusText = findViewById(R.id.edgeStatusText);
        switchEnabled = findViewById(R.id.switchEnabled);
        labelConf = findViewById(R.id.labelConf);
        sliderConf = findViewById(R.id.sliderConf);
        editCooldown = findViewById(R.id.editCooldown);
        labelTargets = findViewById(R.id.labelTargets);
        targetsHint = findViewById(R.id.targetsHint);
        chipGroupTargets = findViewById(R.id.chipGroupTargets);
        statusText = findViewById(R.id.statusText);
        btnSave = findViewById(R.id.btnSave);

        sliderConf.addOnChangeListener((slider, value, fromUser) -> updateConfLabel(value));
        updateConfLabel(sliderConf.getValue());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearTargets).setOnClickListener(v -> {
            chipGroupTargets.clearCheck();
            updateTargetsLabel();
        });
        btnSave.setOnClickListener(v -> startSave());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(statusRefresher);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statusRefresher);
    }

    private void updateConfLabel(float percent) {
        labelConf.setText(String.format(Locale.US, "%s  %.2f", getString(R.string.label_conf_threshold), percent / 100));
    }

    private void updateTargetsLabel() {
        int count = chipGroupTargets.getCheckedChipIds().size();
        labelTargets.setText(count == 0
                ? getString(R.string.label_target_classes) + "  (전체)"
                : getString(R.string.label_target_classes) + "  (" + count + "개)");
    }

    private void showEdgeStatus(JSONObject config) {
        boolean online = config.optBoolean("edge_online");
        edgeStatusText.setText(MonitorConfigApi.describeEdge(config));
        ViewCompat.setBackgroundTintList(edgeStatusDot, ColorStateList.valueOf(
                ContextCompat.getColor(this, online ? R.color.pb_online : R.color.pb_hint)));
    }

    /** 서버에서 받은 설정으로 폼을 채운다. */
    private void fillForm(JSONObject config) {
        switchEnabled.setChecked(config.optBoolean("enabled", true));

        // Slider는 valueFrom + n*stepSize 값만 허용하므로 가장 가까운 단계로 맞춘다
        int percent = (int) Math.round(config.optDouble("conf_threshold", 0.5) * 100 / CONF_STEP_PCT) * CONF_STEP_PCT;
        sliderConf.setValue(Math.min(Math.max(percent, CONF_MIN_PCT), CONF_MAX_PCT));

        editCooldown.setText(String.valueOf(config.optInt("cooldown_sec", 0)));

        Set<String> targets = toSet(config.optJSONArray("target_classes"));
        JSONArray available = config.optJSONArray("available_classes");
        chipGroupTargets.removeAllViews();
        if (available == null || available.length() == 0) {
            targetsHint.setText(R.string.desc_no_available_classes);
        } else {
            for (int i = 0; i < available.length(); i++) {
                String name = available.optString(i);
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_target_chip, chipGroupTargets, false);
                chip.setText(name);
                chip.setChecked(targets.contains(name));
                chip.setOnCheckedChangeListener((button, checked) -> updateTargetsLabel());
                chipGroupTargets.addView(chip);
            }
        }
        updateTargetsLabel();
        btnSave.setEnabled(true);
    }

    private static Set<String> toSet(JSONArray array) {
        Set<String> set = new HashSet<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                set.add(array.optString(i));
            }
        }
        return set;
    }

    private void startSave() {
        String cooldownText = editCooldown.getText().toString().trim();
        int cooldown = TextUtils.isEmpty(cooldownText) ? 0 : Integer.parseInt(cooldownText);

        List<String> targets = new ArrayList<>();
        for (int id : chipGroupTargets.getCheckedChipIds()) {
            Chip chip = chipGroupTargets.findViewById(id);
            targets.add(chip.getText().toString());
        }

        JSONObject body = new JSONObject();
        try {
            body.put("enabled", switchEnabled.isChecked());
            body.put("target_classes", new JSONArray(targets));
            body.put("conf_threshold", Math.round(sliderConf.getValue()) / 100.0);
            body.put("cooldown_sec", cooldown);
        } catch (JSONException e) {
            statusText.setText("저장 실패: " + e.getMessage());
            return;
        }

        btnSave.setEnabled(false);
        statusText.setText("저장 중...");
        new SaveConfigTask(body).execute();
    }

    /** 설정 조회. 첫 응답은 폼을 채우고, 이후에는 Edge 상태만 갱신한다. */
    private class LoadConfigTask extends AsyncTask<Void, Void, JSONObject> {
        private String errorMessage;

        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                return MonitorConfigApi.fetch();
            } catch (MonitorConfigApi.ApiException e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject config) {
            if (isFinishing()) {
                return;
            }
            if (config == null) {
                edgeStatusText.setText("설정을 불러올 수 없음: " + errorMessage);
                ViewCompat.setBackgroundTintList(edgeStatusDot,
                        ColorStateList.valueOf(ContextCompat.getColor(SettingsActivity.this, R.color.pb_error)));
                return;
            }
            showEdgeStatus(config);
            if (!formLoaded) {
                formLoaded = true;
                fillForm(config);
            }
        }
    }

    private class SaveConfigTask extends AsyncTask<Void, Void, JSONObject> {
        private final JSONObject body;
        private String errorMessage;

        SaveConfigTask(JSONObject body) {
            this.body = body;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                return MonitorConfigApi.save(body);
            } catch (MonitorConfigApi.ApiException e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject config) {
            btnSave.setEnabled(true);
            if (config == null) {
                statusText.setText("저장 실패: " + errorMessage);
                return;
            }
            showEdgeStatus(config);
            statusText.setText(R.string.settings_saved);
            Toast.makeText(SettingsActivity.this, R.string.settings_saved, Toast.LENGTH_LONG).show();
        }
    }
}
