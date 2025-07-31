package com.projeto.gertecserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.InetSocketAddress;

public class MyWebSocketServer extends WebSocketServer{
    private WebSocketService context;
    private CliSiTef clisitef;

    public static boolean isConfigured = false;

    public MyWebSocketServer(int port,WebSocketService context){
        super(new InetSocketAddress(port));
        this.context = context;

//        SharedPreferences prefs = context.getSharedPreferences("ws_config", Context.MODE_PRIVATE);
//        isConfigured = prefs.getBoolean("isConfigured", false);
    }

    @Override
    public void onOpen(WebSocket conn,ClientHandshake handshake){
        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status","cliente conectado");
            conn.send(jsonResponse.toString());
        } catch(JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        try {
            JSONObject json = new JSONObject(message);
            String activity = json.optString("activity");

            if("Printer".equals(activity)){
                Impressora impressora = new Impressora(context);
                impressora.imprimir(json);
            }

            if("CliSiTef".equals(activity)){
                if(!isConfigured){
                    clisitef = new CliSiTef(context,conn);
//                    SharedPreferences prefs = context.getSharedPreferences("ws_config", Context.MODE_PRIVATE);
//                    prefs.edit().putBoolean("isConfigured", true).apply();

                    // Resetar isConfigured
                    // SharedPreferences prefs = context.getSharedPreferences("ws_config", Context.MODE_PRIVATE);
                    // prefs.edit().clear().apply();
                }
                clisitef.transaction(json);
            }

            if("continueTransaction".equals(activity)){
                String CMD = json.getBoolean("CMD") ? "CMD_GET_FIELD" : "";
                clisitef.continueTransaction(json.getString("return"),CMD);
            }

        } catch(Exception e){ e.printStackTrace(); }
    }

    @Override
    public void onClose(WebSocket conn,int code,String reason,boolean remote){ restartServer(); }

    @Override
    public void onError(WebSocket conn,Exception ex){
        Log.d("MyWebSocketServer","onError: " + ex.getMessage());
        restartServer();

        if(conn != null && conn.isOpen()){
            try {
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", "erro");
                jsonResponse.put("mensagem", ex.getMessage());
                conn.send(jsonResponse.toString());
            } catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart(){}

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

