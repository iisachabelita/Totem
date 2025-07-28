package com.projeto.gertecserver;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends android.app.Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        startService(new Intent(this,WebSocketService.class));
        finish();
    }
}
