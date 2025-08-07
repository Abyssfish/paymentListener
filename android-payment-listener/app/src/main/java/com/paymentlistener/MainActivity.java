package com.paymentlistener;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.text.TextUtils;

public class MainActivity extends AppCompatActivity {

    private Button enableAccessibilityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableAccessibilityButton = findViewById(R.id.enable_accessibility_button);
        enableAccessibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessibilityStatus();
    }

    private void updateAccessibilityStatus() {
        if (isAccessibilityServiceEnabled()) {
            enableAccessibilityButton.setText("无障碍服务已启用");
            enableAccessibilityButton.setEnabled(false);
            Toast.makeText(this, "支付监听服务已启动", Toast.LENGTH_SHORT).show();
        } else {
            enableAccessibilityButton.setText("启用无障碍服务");
            enableAccessibilityButton.setEnabled(true);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + PaymentAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String accessibilityService = colonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}