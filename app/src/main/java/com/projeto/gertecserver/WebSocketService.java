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

public class WebSocketService extends Service {
    private MyWebSocketServer server;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(
                "gertec_channel",
                "Servidor WebSocket",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);

        // Notificação silenciosa
        Notification notification = new NotificationCompat.Builder(this, "gertec_channel")
                .setContentTitle("Servidor WebSocket ativo")
                .setContentText("Rodando em segundo plano")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Substitua por seu ícone
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification); // Ativa o serviço em modo foreground

        server = new MyWebSocketServer(2235, this); // Porta
        server.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Garante que o serviço seja reiniciado se for encerrado
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            server.stop(); // Para o servidor
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}