package com.projeto.gertecserver;

import android.content.Context;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebViewInterface {
    Context mContext;
    CliSiTef clisitef;

    WebViewInterface(Context context) { this.mContext = context; }

    @JavascriptInterface
    public void configure(String message) throws Exception {
        JSONObject json = new JSONObject(message);
        clisitef = new CliSiTef(mContext);
        clisitef.configurarCliSiTef(json);
        new Impressora(mContext).configurarImpressora(json);
    }

    @JavascriptInterface
    public void transaction(String message) throws Exception {
        JSONObject json = new JSONObject(message);
        clisitef.transaction(json);
    }

    @JavascriptInterface
    public void printer(String message) throws Exception {
        JSONObject json = new JSONObject(message);
        Impressora impressora = new Impressora(mContext);
        JSONObject parameters = json.optJSONObject("parameters");
        JSONArray items = json.optJSONArray("items");
        impressora.imprimirComprovante(items, parameters);
    }
}