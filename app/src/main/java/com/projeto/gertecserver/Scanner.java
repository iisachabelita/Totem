package com.projeto.gertecserver;

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
        Log.e("Scanner", "Iniciando leitura no scanner embutido...");

        scanner = CodeScanner.getInstance(this);

        // Inicia o scanner agora
        scanner.scanCode(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.e("Scanner", "Scanner timeout - parando leitura.");
            scanner.stopService();
            finish();
        }, 30000);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("onActivityResult", "requestCode: " + requestCode + ", resultCode: " + resultCode);
    }

    @Override
    protected void onPause() {
        Log.e("onPause", "Pausando scanner...");
        super.onPause();
    }
}