package com.projeto.totemserver;

import android.app.Activity;
import android.content.SharedPreferences;
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
    CliSiTef clisitef;

    private static final String LOGIN_URL = "https://isabelly-deeliv.felippebueno.com.br/totem/globais/login";

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

        geckoSession.loadUri(LOGIN_URL);
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
                        new Impressora(this).configurarImpressora(payload);
                        configureSent = true;
                    }
                    break;
                case "transaction":
                    clisitef.transaction(payload);
                    break;
                case "printer":
                    Impressora impressora = new Impressora(this);
                    JSONObject parameters = payload.optJSONObject("parameters");
                    JSONArray items = payload.optJSONArray("items");
                    impressora.imprimirComprovante(items, parameters);
                    break;
            }

        } catch (Exception e){ Log.e("Bridge", "Erro ao processar mensagem do JS", e); }
    }

    public static void sendToJS(JSONObject json){
        try {
            String js = "window.postMessage(" + JSONObject.quote(json.toString()) + ", '*');";
            geckoSession.loadUri("javascript:" + js);
            Log.d("Bridge", "JS enviado: " + js);
        } catch (Exception e){ Log.e("Bridge", "Erro ao enviar JS", e); }
    }
}
