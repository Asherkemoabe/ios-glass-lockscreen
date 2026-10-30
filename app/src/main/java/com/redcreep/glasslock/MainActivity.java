package com.redcreep.glasslock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final int FILE_REQ = 1;
    private static final String HOOK = "(function(){if(window.__h||typeof unlock!=='function')return;window.__h=1;var u=unlock;unlock=function(){u();setTimeout(function(){Android.done()},420)}})();";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private boolean lockMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lockMode = getIntent().getBooleanExtra("lock", false);
        applyLockFlags();

        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (lockMode) {
                    view.evaluateJavascript(HOOK, null);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQ);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        loadPage();
    }

    private void loadPage() {
        web.loadUrl(lockMode ? "file:///android_asset/index.html" : "file:///android_asset/setup.html");
    }

    private void applyLockFlags() {
        int f = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        if (lockMode) {
            getWindow().addFlags(f);
        } else {
            getWindow().clearFlags(f);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        lockMode = intent.getBooleanExtra("lock", false);
        applyLockFlags();
        loadPage();
    }

    private void finishUnlock() {
        if (Build.VERSION.SDK_INT >= 26) {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissSucceeded() {
                    finish();
                }

                @Override
                public void onDismissCancelled() {
                    finish();
                }

                @Override
                public void onDismissError() {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("g", Context.MODE_PRIVATE);
    }

    private void appSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private class Bridge {
        @JavascriptInterface
        public void done() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finishUnlock();
                }
            });
        }

        @JavascriptInterface
        public boolean isOn() {
            return prefs().getBoolean("on", false);
        }

        @JavascriptInterface
        public void enable() {
            prefs().edit().putBoolean("on", true).apply();
            LockService.start(MainActivity.this);
        }

        @JavascriptInterface
        public void disable() {
            prefs().edit().putBoolean("on", false).apply();
            stopService(new Intent(MainActivity.this, LockService.class));
        }

        @JavascriptInterface
        public void preview() {
            LockService.launch(MainActivity.this);
        }

        @JavascriptInterface
        public void battery() {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                appSettings();
            }
        }

        @JavascriptInterface
        public void launchMgr() {
            try {
                Intent i = new Intent();
                i.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                startActivity(i);
            } catch (Exception e) {
                appSettings();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!lockMode) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQ && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }
}
