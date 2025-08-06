package com.projeto.gertecserver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.InetSocketAddress;

public class MyWebSocketServer extends WebSocketServer{
    private WebSocketService context;

    public static boolean isConfigured = false;
    public static String cupom;

    public MyWebSocketServer(int port,WebSocketService context){
        super(new InetSocketAddress(port));
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn,ClientHandshake handshake){
        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status","cliente conectado");
            conn.send(jsonResponse.toString());
        } catch(JSONException e){}
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        try {
            JSONObject json = new JSONObject(message);
            String activity = json.optString("activity");

            if("Printer".equals(activity)){
                String image = json.optString("image");
                byte[] imageBytes = Base64.decode(image,Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                if(bitmap != null){
                    Impressora impressora = new Impressora(context);
                    impressora.imprimirComprovante(bitmap);
                }
            }

            if("CliSiTef".equals(activity)){
                if(WebSocketService.clisitef == null){
                    WebSocketService.clisitef = new CliSiTef(context,conn);
                } else{
                    WebSocketService.clisitef.setWebSocket(conn);
                }

                WebSocketService.clisitef.transaction(json);
            }

            if("continueTransaction".equals(activity)){
                WebSocketService.clisitef.continueTransaction(json.getString("return"));
            }

            if("Scanner".equals(activity)){
//                if(WebSocketService.scanner == null){
                    WebSocketService.scanner = new Intent(context,Scanner.class);
//                }

                WebSocketService.scanner.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(WebSocketService.scanner);
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
                jsonResponse.put("status", "erro");
                jsonResponse.put("mensagem", ex.getMessage());
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
            try {
                Thread.sleep(2000);
                this.start();
            } catch(Exception e){ e.printStackTrace(); }
        }).start();
    }
}

