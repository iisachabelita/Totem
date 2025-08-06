package com.projeto.gertecserver;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        startService(new Intent(this,WebSocketService.class));
        finish();
    }
}
