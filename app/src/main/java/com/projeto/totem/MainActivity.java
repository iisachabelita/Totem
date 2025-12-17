package com.projeto.totem;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

public class MainActivity extends Activity {
    private static GeckoSession geckoSession;
    private GeckoRuntime geckoRuntime;
    private boolean configureSent = false;
    static CliSiTef clisitef;
    static Impressora impressora = null;
    private static WebExtension.Port mPort;
    // private static final String LOGIN_URL = "https://www.deeliv.app/totem/globais/login";
    private static final String LOGIN_URL = "https://isabelly-deeliv.felippebueno.com.br/totem/globais/login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        GeckoView geckoView = new GeckoView(this);
        setContentView(geckoView);

        if(geckoRuntime == null){
            geckoRuntime = GeckoRuntime.create(this);
            installExtension();
        }

        geckoSession = new GeckoSession();
        geckoSession.open(geckoRuntime);

        geckoView.setSession(geckoSession);

        // Fullscreen
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if(controller != null){
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        loadFreshUrl();
    }

    void installExtension(){
        geckoRuntime.getWebExtensionController()
            .ensureBuiltIn("resource://android/assets/extension/", "gecko@projeto.com")
            .accept(
                extension -> {
                    runOnUiThread(() -> extension.setMessageDelegate(mMessagingDelegate, "browser"));
                },
                e -> Log.e("MessageDelegate", "Error registering WebExtension", e)
            );
    }

    private final WebExtension.MessageDelegate mMessagingDelegate = new WebExtension.MessageDelegate(){
        @Nullable
        @Override
        public void onConnect(@NonNull WebExtension.Port port){
        mPort = port;
        mPort.setDelegate(mPortDelegate);
        }
    };

    private final WebExtension.PortDelegate mPortDelegate = new WebExtension.PortDelegate(){
        @Override
        public void onPortMessage(final @NonNull Object message, final @NonNull WebExtension.Port port){
            runOnUiThread(() -> {
                try {
                    // Converter a mensagem recebida para JSONObject
                    JSONObject json = (message instanceof JSONObject) ? (JSONObject) message : new JSONObject(message.toString());

                    if("JSBridge".equals(json.optString("action"))){
                        Object dataObj = json.get("data");

                        if(dataObj instanceof JSONObject){
                            JSONObject dataJson = (JSONObject) dataObj;

                            String command = dataJson.optString("command");
                            JSONObject payload = dataJson.optJSONObject("payload"); // Pode ser String, JSONObject ou JSONArray

                            handleBridgeMessage(command, payload);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Bridge", "Erro ao processar mensagem: " + e.getMessage());
                }
            });
        }

        @Override
        public void onDisconnect(@NonNull WebExtension.Port port){
            mPort = null;
        }
    };

    private void loadFreshUrl(){
        String url = LOGIN_URL + "?t=" + System.currentTimeMillis();
        geckoSession.loadUri(url);
    }

    private void handleBridgeMessage(String command, JSONObject payload){
        try {
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
        } catch (Exception e) {
            Log.e("Bridge", "handleBridgeMessage: ", e);
        }
    }

    public static void sendToJS(JSONObject json){
        try {
            String js = "window.postMessage(" + json + ", '*');";
            geckoSession.loadUri("javascript:" + js);
        } catch (Exception e) {
            Log.e("Bridge", "sendToJS: ", e);
        }
    }
}
