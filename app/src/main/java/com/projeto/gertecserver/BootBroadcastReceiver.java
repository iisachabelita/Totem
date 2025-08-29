//package com.projeto.gertecserver;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//
//import androidx.core.content.ContextCompat;
//
//public class BootBroadcastReceiver extends BroadcastReceiver {
//    @Override
//    public void onReceive(Context context, Intent intent){
//        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
//            Intent serviceIntent = new Intent(context, WebSocketService.class);
//            ContextCompat.startForegroundService(context, serviceIntent);
//        }
//    }
//}
