package com.projeto.gertecserver;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class MyWebSocketServer extends WebSocketServer {
    private final WebSocketService context;
    public MyWebSocketServer(int port, WebSocketService context) {
        super(new InetSocketAddress(port));
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake){
        conn.send("{\"status\":\"conectado\"}"); // Mensagem de teste
        System.out.println("Cliente conectado");
    }

    @Override
    public void onMessage(WebSocket conn, String message){
        try {
            JSONObject json = new JSONObject(message);
            String activity = json.optString("activity");

            if("Printer".equals(activity)){
                Impressora impressora = new Impressora(context);
                String resposta = impressora.imprimir(json);
                conn.send(resposta);
            }

            if("CliSiTef".equals(activity)){
                CliSiTef cliSiTef = new CliSiTef(context);
                String resposta = cliSiTef.transaction(json);
                conn.send(resposta);
            }

        } catch(Exception e){
            conn.send("{\"status\":\"erro\",\"mensagem\":\"JSON inválido ou erro interno\"}");
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Desconectado: " + reason);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {

    }
}

