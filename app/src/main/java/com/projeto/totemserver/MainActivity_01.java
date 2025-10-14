package com.projeto.totemserver;

import android.os.Bundle;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity_01 extends Activity{
    private static WebView webView;

    private static final String LOGIN_URL = "https://isabelly-deeliv.felippebueno.com.br/totem/globais/login";
    // private static final String LOGIN_URL = "https://www.deeliv.app/totem/globais/login";
    private boolean configureSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(webView);

        // Configura WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url){
            super.onPageFinished(view, url);

            if(!url.equals(LOGIN_URL) && !configureSent){
                sendConfigureCommand();
                configureSent = true;
            }
            }
        });

        webView.addJavascriptInterface(new WebViewInterface(this), "Bridge");

        // Fullscreen
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if(controller != null){
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        webView.loadUrl(LOGIN_URL);
    }

    private void sendConfigureCommand() {
        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("command", "configure");
            send(jsonResponse.toString());
        } catch (JSONException ignored) {}
    }

    public static void send(String message){
        String jsCode = "handleBridgeMessage(" + JSONObject.quote(message) + ")";
        webView.evaluateJavascript(jsCode, null);
    }
}