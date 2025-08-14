package com.projeto.gertecserver;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity{
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        startService(new Intent(this,WebSocketService.class));
        // finish();

        // Cria WebView programaticamente
        webView = new WebView(this);

        // Configurar layout params full screen
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Configura WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // Para WSS/HTTPS misto

        webView.setWebViewClient(new WebViewClient()); // Abre links na própria WebView

        // Define como conteúdo da Activity
        setContentView(webView);

        // Fullscreen real
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if(controller != null){
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        // Remover ActionBar (se houver)
        if(getActionBar() != null) getActionBar().hide();

        webView.loadUrl("https://isabelly-deeliv.felippebueno.com.br/totem/globais/login");
    }

    @Override
    public void onBackPressed() {
        if(webView != null && webView.canGoBack()) {
            webView.goBack(); // Navega na WebView
        } else {
            super.onBackPressed(); // Fecha Activity normalmente
        }
    }
}
