package com.paymentlistener;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private static final String TAG = "PaymentListener";
    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;
    private Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }

        // 获取通知包名
        String packageName = String.valueOf(event.getPackageName());
        
        // 检查是否是支付宝或微信的通知
        if (!packageName.equals(ALIPAY_PACKAGE) && !packageName.equals(WECHAT_PACKAGE)) {
            return;
        }

        // 获取通知内容
        String content = "";
        if (event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            Bundle extras = notification.extras;
            content = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();
        }

        // 处理支付信息
        handlePaymentNotification(packageName, content);
    }

    private void handlePaymentNotification(String packageName, String content) {
        Log.d(TAG, "收到通知: " + packageName + " - " + content);
        
        // 提取金额
        String amount = extractAmount(content);
        if (amount == null) {
            Log.d(TAG, "未找到金额信息");
            return;
        }

        // 判断是收款还是付款
        boolean isIncome = false;
        if (packageName.equals(ALIPAY_PACKAGE)) {
            isIncome = content.contains("收款") || content.contains("成功收入") || content.contains("到账");
        } else if (packageName.equals(WECHAT_PACKAGE)) {
            isIncome = content.contains("微信支付收款") || content.contains("收到转账") || content.contains("收款到账通知");
        }

        if (isIncome) {
            // 语音播报收款信息
            speak("收到" + (packageName.equals(ALIPAY_PACKAGE) ? "支付宝" : "微信") + "付款" + amount + "元");
            Log.d(TAG, "收款播报: " + amount + "元");
        }
    }

    private String extractAmount(String content) {
        // 使用正则表达式提取金额
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void speak(final String text) {
        if (!isTtsInitialized) {
            // 如果TTS未初始化，延迟执行
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    speak(text);
                }
            }, 1000);
            return;
        }

        // 执行语音播报
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "payment_notification");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "语言不支持");
            } else {
                isTtsInitialized = true;
                Log.d(TAG, "TTS初始化成功");
            }
        } else {
            Log.e(TAG, "TTS初始化失败");
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "服务中断");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "服务已连接");
        
        // 初始化TTS
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务已销毁");
        
        // 释放TTS资源
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}    