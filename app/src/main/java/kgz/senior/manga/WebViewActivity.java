package kgz.senior.manga;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.SharedPreferences;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final String PREFS_NAME = "WebViewState";
    private static final String KEY_COOKIES = "cookies";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        hideSystemUI();

        webView = findViewById(R.id.webview);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Оптимизация настроек WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // Сохранение данных между сессиями
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDatabasePath(getApplicationContext().getCacheDir().getAbsolutePath());

        // Настройка cookies для сохранения авторизации
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        // Восстановление сохраненных cookies если они есть
        restoreCookies(cookieManager);

        // Получаем URL из Intent
        String url = getIntent().getStringExtra("URL");
        if (url != null) {
            webView.loadUrl(url);
        }

        // Оптимизированная обработка свайпа
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (webView.getProgress() == 100) {
                webView.reload();
            }
        });

        // Обработка кнопки "Назад"
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            return false;
        });

        // Оптимизированный WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                // Сохраняем cookies после каждой загрузки страницы
                saveCookies();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }
    
    // Сохранение cookies для поддержания авторизации
    private void saveCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(webView.getUrl());
        if (cookies != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString(KEY_COOKIES, cookies).apply();
        }
    }
    
    // Восстановление cookies при запуске
    private void restoreCookies(CookieManager cookieManager) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cookies = prefs.getString(KEY_COOKIES, null);
        if (cookies != null) {
            // Парсим сохраненные cookies и восстанавливаем их
            String[] cookieValues = cookies.split(";");
            for (String cookie : cookieValues) {
                cookieManager.setCookie(getHostFromCookie(cookie), cookie.trim());
            }
            cookieManager.flush();
        }
    }
    
    // Извлечение домена из cookie
    private String getHostFromCookie(String cookie) {
        // По умолчанию возвращаем домен из URL
        String url = webView.getUrl();
        if (url != null) {
            return url;
        }
        
        // Если URL недоступен, пытаемся получить домен из cookies
        if (cookie.contains("domain=")) {
            String[] parts = cookie.split("domain=");
            if (parts.length > 1) {
                String domain = parts[1].trim();
                if (domain.startsWith(".")) {
                    domain = domain.substring(1);
                }
                if (domain.contains(";")) {
                    domain = domain.split(";")[0];
                }
                return "https://" + domain;
            }
        }
        
        // Если не можем определить домен, используем общий домен
        return "https://senkuro.com";
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем cookies при приостановке активности
        saveCookies();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            // Сохраняем cookies перед уничтожением WebView
            saveCookies();
            
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            
            // Очищаем кэш, но НЕ очищаем cookies
            webView.clearFormData();
        }
        super.onDestroy();
    }
}
