package com.projeto.totemserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import br.com.gertec.easylayer.codescanner.CodeScanner;

public class Scanner extends Activity {
    private CodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Scanner", "Iniciando leitura no scanner embutido...");

        scanner = CodeScanner.getInstance(this);

        // Inicia o scanner agora
        scanner.scanCode(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("Scanner", "Scanner timeout - parando leitura.");
            scanner.stopService();
            finish();
        }, 30000);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "requestCode: " + requestCode + ", resultCode: " + resultCode);
    }

    @Override
    protected void onPause() {
        Log.d("onPause", "Pausando scanner...");
        super.onPause();
    }
}