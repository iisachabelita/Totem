package com.projeto.gertecserver;

import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_RESULT_DATA;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.softwareexpress.sitef.android.ICliSiTefListener;
import br.com.softwareexpress.sitef.android.modules.IPinPad;

public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    private final br.com.softwareexpress.sitef.android.CliSiTef clisitef;
    private final WebSocket conn;
    private boolean isConfigured = false;

    public CliSiTef(Context context,WebSocket conn){
        this.context = context.getApplicationContext();
        this.clisitef = new br.com.softwareexpress.sitef.android.CliSiTef(this.context);
        this.conn = conn;
    }

    public int configurarCliSiTef(JSONObject json){
        String IPSiTef = json.optString("IPSiTef");
        String IdLoja = json.optString("IdLoja");
        String IdTerminal = json.optString("IdTerminal");
//        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;TipoComunicacaoExterna=SSL;]";
        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;]";

        int config = clisitef.configure(IPSiTef,IdLoja,IdTerminal,ParametrosAdicionais);

        if(config == 0){
            this.isConfigured = true;
            Log.e("CliSiTef", "CliSiTef configurado com sucesso");
        } else{
            Log.e("CliSiTef", "Falha ao configurar CliSiTef. Código: " + config);
        }

        return config;
    }

    public void transaction(JSONObject json){
        if(!isConfigured){
            int config = configurarCliSiTef(json);
            if(config != 0){
                conn.send("Erro ao configurar CliSiTef");
                return;
            }
        }

        try {
            boolean teste = clisitef.pinpad.isPresent();
            Log.e("CliSiTef", "PINPAD PRESENT: " + teste);
//            clisitef.pinpad.setDisplayMessage("start transaction...",true);
        } catch(Exception e){
            Log.e("CliSiTef", "PINPAD ERRO: " + e.getMessage());
        }

        JSONObject parameters = json.optJSONObject("parameters");
        int modalidade = parameters.optInt("modalidade");
        String valor = parameters.optString("valor");
        String docFiscal = parameters.optString("docFiscal");
        String dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
        String operador = parameters.optString("operador");
        String restricoes = parameters.optString("restricoes");
//        clisitef.pinpad.readYesNo()

//        int status = clisitef.startTransaction(this,modalidade,valor,docFiscal,dataFiscal,horaFiscal,operador,restricoes);
//        Log.e("CliSiTef", "START TRANSACTION: " + status);

        int status = clisitef.startTransaction(this, modalidade, valor, docFiscal, dataFiscal, horaFiscal, operador, restricoes);
        Log.e("CliSiTef", "START TRANSACTION: " + status);
    }

//    public void pinpad(JSONObject json){
//        if(!isConfigured){
//            int config = configurarCliSiTef(json);
//            if(config != 0){
//                conn.send("Erro ao configurar CliSiTef");
//                return;
//            }
//        }
//
//        int teste1 = clisitef.pinpad.setDisplayMessage("Iniciando transação... teste 1");
//        Log.e("CliSiTef", "PINPAD: " + teste1);
//
//        try {
//            boolean teste = clisitef.pinpad.isPresent();
//            Log.e("CliSiTef", "PINPAD: " + teste);
//            clisitef.pinpad.setDisplayMessage("Iniciando transação...");
//        } catch(Exception e){
//            Log.e("CliSiTef", "Erro ao acessar PINPAD: " + e.getMessage());
//        }
//    }

    @Override
    public void onData(
        int stage,       // Etapa da transação
        int command,     // Comando a ser executado
        int fieldId,     // Identificador do dado
        int minLength,   // Mínimo de caracteres esperados
        int maxLength,   // Máximo de caracteres esperados
        byte[] input     // Dados de entrada (geralmente nulos na ida, preenchidos na volta)
    ){
        Log.e("CliSiTef", "onData, stage: " + stage + " command: " + command + " fieldId: " + fieldId + " minLength: " + minLength + " maxLength: " + maxLength + " input: " + new String(input));

        switch(command){
            case CMD_RESULT_DATA:

        }

        if(command == 0){
            clisitef.continueTransaction("");
        } else{
//            continueTransaction(1,data);
        }
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        Log.d("CliSiTef","onTransactionResult, stage " + stage + " resultCode: " + resultCode);

        clisitef.finishTransaction(1, "","", "", "");

        JSONObject response = new JSONObject();
        try {
            response.put("status", "finalizado");
            response.put("codigo", resultCode);
        } catch (Exception e){
            Log.e("CliSiTef", "Erro JSON: " + e.getMessage());
        }
        conn.send(response.toString());
    }

    public void continueTransaction(int stage, String data){
        // Enviando dados para o totem
        if(stage == 1){
            conn.send(data);
        }

        // Coletando dados no pinpad
        if(stage == 2){
            int status = clisitef.continueTransaction(data);
            Log.e("CliSiTef", "CONTINUE TRANSACTION: " + status);
        }
    }
}