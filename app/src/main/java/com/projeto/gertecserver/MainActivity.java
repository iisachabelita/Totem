package com.projeto.gertecserver;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity{
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // startService(new Intent(this,WebSocketService.class));
        ContextCompat.startForegroundService(this,new Intent(this,WebSocketService.class));

        webView = new WebView(this);

        // Fullscreen
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Configura WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        // webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // Para WSS/HTTPS misto

        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);

        // Fullscreen
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if(controller != null){
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        webView.loadUrl("https://isabelly-deeliv.felippebueno.com.br/totem/globais/login");
    }

    @Override
    public void onBackPressed() {
        if(webView != null && webView.canGoBack()) {
            webView.goBack(); // Navega na WebView
        } else{
            super.onBackPressed(); // Fecha Activity normalmente
        }
    }
}
