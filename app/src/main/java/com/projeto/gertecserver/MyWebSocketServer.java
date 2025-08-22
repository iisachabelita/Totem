package com.projeto.gertecserver;

import static com.projeto.gertecserver.WebSocketService.clisitef;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
    public static String cupom;

    public MyWebSocketServer(int port,WebSocketService context){
        super(new InetSocketAddress(port));
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn,ClientHandshake handshake){
        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("command","configure");
            conn.send(jsonResponse.toString());
        } catch(JSONException e){}
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        try {
            JSONObject json = new JSONObject(message);
            String activity = json.optString("activity");

            switch(activity){
                case "configure":
                    handleTef("configure",json,conn);
                    break;
                case "transaction":
                    handleTef("transaction",json,conn);
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
        Log.d("MyWebSocketServer","onError: " + ex.getMessage());

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

        new Thread(() -> {
            int retries = 0;
            while (retries < 5) {
                try {
                    Thread.sleep(5000 * retries);
                    this.start();
                    break;
                } catch(Exception e) {
                    retries++;
                    Log.e("WebSocket", "Tentando reconectar: " + retries);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleTef(String activity,JSONObject json,WebSocket conn) throws JSONException {
        switch(activity){
            case "configure":
                if(clisitef == null){
                    clisitef = new CliSiTef(context,conn);
                } else{
                    clisitef.setWebSocket(conn);
                }

                if(!clisitef.configureCliSiTef){
                    clisitef.configurarCliSiTef(json,false);
                }
                break;
            case "transaction":
                clisitef.transaction(json);
                break;
        }
    }

    private void handlePrinter(JSONObject json) throws JSONException, PrinterException {
        String image = json.optString("image");
        JSONObject parameters = json.optJSONObject("parameters");
        JSONArray items = json.optJSONArray("items");
        byte[] imageBytes = Base64.decode(image,Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        if(bitmap != null){
            Impressora impressora = new Impressora(context);
            impressora.imprimirComprovante(bitmap,items,parameters);
        }
    }

    private void handleScanner() throws JSONException {
        // WebSocketService.scanner = new Intent(context,Scanner.class);
        // WebSocketService.scanner.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // context.startActivity(WebSocketService.scanner);
    }
}

