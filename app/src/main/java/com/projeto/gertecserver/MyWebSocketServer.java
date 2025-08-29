package com.projeto.gertecserver;

import static com.projeto.gertecserver.WebSocketService.clisitef;

import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import br.com.gertec.easylayer.printer.PrinterException;

public class MyWebSocketServer extends WebSocketServer{
    private WebSocketService context;
    public static boolean configureCliSiTef = false;

    public MyWebSocketServer(int port,WebSocketService context){
        super(new InetSocketAddress(port));
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn,ClientHandshake handshake){
        if(!configureCliSiTef){
            JSONObject obj = new JSONObject();
            try {
                obj.put("command", "configure");
            } catch(JSONException e){ e.printStackTrace(); }

            conn.send(obj.toString());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        try {
            JSONObject json = new JSONObject(message);
            String activity = json.optString("activity");

            switch(activity){
                case "configure":
                    handleConfig(json,conn);
                    break;
                case "transaction":
                    handleTef(json,conn);
                    break;
                case "printer":
                    handlePrinter(json);
                    break;
            }
        } catch(Exception e){
            Log.e("WebSocket", "Erro ao processar mensagem: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn,int code,String reason,boolean remote){
        Log.d("WebSocket", "Pronto para aceitar novas conexões.");
    }

    @Override
    public void onError(WebSocket conn,Exception ex){
        Log.e("MyWebSocketServer","onError: " + ex.getMessage());

        if(conn != null && conn.isOpen()){
            try {
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("message", ex.getMessage());
                conn.send(jsonResponse.toString());
            } catch(JSONException e){}
        }

        restartServer();
    }

    @Override
    public void onStart(){
        Log.d("WebSocket", "Servidor WebSocket iniciado e escutando em: " + getAddress());
    }

    private void restartServer(){
        try { this.stop(); } catch(Exception ignored){}

        configureCliSiTef = false;
    }

    private void setCliSiTef(WebSocket conn){
        if(clisitef == null){
            clisitef = new CliSiTef(context,conn);
        } else{
            clisitef.setWebSocket(conn);
        }
    }

    private void handleConfig(JSONObject json,WebSocket conn) throws Exception {
        Log.d("WebSocket", "Configurando: " + json.toString());

        setCliSiTef(conn);

        clisitef.configurarCliSiTef(json);
        Impressora impressora = new Impressora(context);
        impressora.configurarImpressora(json);
    }
    private void handleTef(JSONObject json,WebSocket conn){
        setCliSiTef(conn);
        clisitef.transaction(json);
    }

    private void handlePrinter(JSONObject json) throws JSONException, PrinterException {
        Impressora impressora = new Impressora(context);
        JSONObject parameters = json.optJSONObject("parameters");
        JSONArray items = json.optJSONArray("items");
        impressora.imprimirComprovante(items,parameters);
    }

    private void handleScanner() throws JSONException {
        // WebSocketService.scanner = new Intent(context,Scanner.class);
        // WebSocketService.scanner.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // context.startActivity(WebSocketService.scanner);
    }
}

