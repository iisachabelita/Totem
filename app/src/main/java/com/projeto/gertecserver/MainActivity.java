package com.projeto.gertecserver;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, WebSocketService.class)); // Inicia o servidor

        finish(); // Fecha imediatamente
    }
}
