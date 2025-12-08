package com.projeto.totem;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {
    private static GeckoSession geckoSession;
    private GeckoRuntime geckoRuntime;
    private boolean configureSent = false;
    static CliSiTef clisitef;
    static Impressora impressora = null;
    // private static final String LOGIN_URL = "https://www.deeliv.app/totem/globais/login";
    // private static final String LOGIN_URL = "https://isabelly-deeliv.felippebueno.com.br/totem/globais/login";
    private static final String LOGIN_URL = "https://www.sandbox-deeliv.app/totem/globais/login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        GeckoView geckoView = new GeckoView(this);
        setContentView(geckoView);

        geckoRuntime = GeckoRuntime.create(this);
        geckoSession = new GeckoSession();
        geckoSession.open(geckoRuntime);

        geckoView.setSession(geckoSession);

        // Comandos JS
        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate(){
            @Override
            public void onTitleChange(GeckoSession session, String title) {
            if (title != null && title.startsWith("bridge:")) {
                    String jsonStr = title.substring("bridge:".length());
                    handleBridgeMessage(jsonStr);
                }
            }
        });

        // Fullscreen
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if(controller != null){
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        loadFreshUrl();
    }

    private void loadFreshUrl(){
        String url = LOGIN_URL + "?t=" + System.currentTimeMillis();
        geckoSession.loadUri(url);
    }

    private void handleBridgeMessage(String jsonStr){
        try {
            JSONObject json = new JSONObject(jsonStr);
            String command = json.optString("command");
            JSONObject payload = json.optJSONObject("payload");

            switch(command){
                case "configure":
                    if(!configureSent){
                        clisitef = new CliSiTef(this);
                        clisitef.configurarCliSiTef(payload);
                        impressora = new Impressora(this);
                        impressora.configurarImpressora(payload);
                        configureSent = true;
                    }
                    break;

                case "transaction":
                    clisitef.transaction(payload);
                    break;

                case "cancelTransaction":
                    // 1 - volta ao menu anterior; -1 - cancela operação;
                    clisitef.clisitef.abortTransaction(-1);
                    break;

                case "printer":
                    JSONObject parameters = payload.optJSONObject("parameters");
                    JSONArray items = payload.optJSONArray("items");
                    impressora.imprimirComprovante(items, parameters);
                    break;

                case "tefReceipt":
                    String action = payload.optString("action");

                    if(action.equals("print")) impressora.imprimirComprovanteTransacao();

                    clisitef.clisitef.finishTransaction(1);
                    break;

                case "managementMenu":
                    clisitef.clisitef.continueTransaction(payload.optString("message"));
                    break;
            }
        } catch (Exception e){ Log.e("Bridge", "handleBridgeMessage: ", e); }
    }

    public static void sendToJS(JSONObject json){
        try {
            String js = "window.postMessage(" + JSONObject.quote(json.toString()) + ", '*');";
            geckoSession.loadUri("javascript:" + js);
        } catch (Exception e){ Log.e("Bridge", "sendToJS: ", e); }
    }
}
