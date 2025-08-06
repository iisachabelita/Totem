package com.projeto.gertecserver;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Notification;
import androidx.core.app.NotificationCompat;
import androidx.annotation.Nullable;

public class WebSocketService extends Service{
    private MyWebSocketServer server;
    public static CliSiTef clisitef;
    public static Intent scanner;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate(){
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(
                "deeliv_channel",
                "Servidor WebSocket",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if(manager != null) manager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this,"deeliv_channel")
                .setContentTitle("Servidor WebSocket ativo")
                .setContentText("Rodando em segundo plano")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);

        server = new MyWebSocketServer(2235,this);
        server.start();
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){ return START_STICKY; }

    @Override
    public void onDestroy(){
        try { server.stop(); } catch(InterruptedException e){ throw new RuntimeException(e); }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){ return null; }
}