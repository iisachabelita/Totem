package com.projeto.gertecserver;

import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_ABORT_REQUEST;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_HEADER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MENU_TITLE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRMATION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRM_GO_BACK;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_BARCODE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_CHEQUE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_CURRENCY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_INTERNAL;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_PASSWORD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_TRACK;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_MASKED_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_MENU_OPTION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_PINPAD_CONFIRMATION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_MESSAGE_QRCODE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_PRESS_ANY_KEY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_REMOVE_QRCODE_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_RESULT_DATA;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_HEADER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MENU_TITLE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_QRCODE_FIELD;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.softwareexpress.sitef.android.ICliSiTefListener;
import br.com.softwareexpress.sitef.android.modules.IPinPad;
import kotlin.collections.UArraySortingKt;

public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    private final br.com.softwareexpress.sitef.android.CliSiTef clisitef;
    private final WebSocket conn;
    private boolean isConfigured = false;

    private String cupom;

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
                try {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("status","erro ao configurar CliSiTef");
                    conn.send(jsonResponse.toString());
                } catch(JSONException e){}
                return;
            }
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

        if(stage == 1){
            switch(command){
//            case CMD_RESULT_DATA:
//            case CMD_SHOW_MSG_CASHIER:
//            case CMD_SHOW_MSG_CASHIER_CUSTOMER:
//            case CMD_CLEAR_MSG_CASHIER_CUSTOMER:
//            case CMD_SHOW_MSG_CUSTOMER:
//            case CMD_SHOW_MENU_TITLE:
//            case CMD_CLEAR_MSG_CASHIER:
//            case CMD_CLEAR_MSG_CUSTOMER:
//            case CMD_CLEAR_MENU_TITLE:
//            case CMD_SHOW_HEADER:
//            case CMD_CLEAR_HEADER:
//            case CMD_CONFIRM_GO_BACK:
//            case CMD_CONFIRMATION:
                case CMD_GET_MENU_OPTION:
                    try {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("message",new String(input));
                        conn.send(jsonResponse.toString());
                    } catch(JSONException e){}
                    break;
                case CMD_GET_FIELD:

                    try {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("message",new String(input));
                        jsonResponse.put("minLength",minLength);
                        jsonResponse.put("maxLength",maxLength);
                        conn.send(jsonResponse.toString());
                    } catch(JSONException e){}
                    break;
//            case CMD_PRESS_ANY_KEY:
//            case CMD_ABORT_REQUEST:
//            case CMD_GET_FIELD_INTERNAL:
//            case CMD_GET_FIELD_CHEQUE:
//            case CMD_GET_FIELD_TRACK:
//            case CMD_GET_FIELD_PASSWORD:
//            case CMD_GET_FIELD_CURRENCY:
//            case CMD_GET_FIELD_BARCODE:
//            case CMD_GET_PINPAD_CONFIRMATION:
//            case CMD_GET_MASKED_FIELD:
//            case CMD_SHOW_QRCODE_FIELD:
//            case CMD_REMOVE_QRCODE_FIELD:
//            case CMD_MESSAGE_QRCODE:
                default:
                    clisitef.continueTransaction("");
                    break;
            }
        }

        if(fieldId == 121){
            cupom = new String(input);
        }
    }

    public void continueTransaction(String returnTransaction, String CMD){
        if(CMD.equals(CMD_GET_FIELD)){
            clisitef.setBuffer(returnTransaction);
            clisitef.continueTransaction("");
            return;
        }

        clisitef.continueTransaction(returnTransaction);
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        Log.d("CliSiTef","onTransactionResult, stage " + stage + " resultCode: " + resultCode);

        if(resultCode == 0){
            // Impressora impressora = new Impressora(context);
            // impressora.imprimirComprovante(cupom);
        }

        if(stage == 1 && resultCode == 0){
            try { clisitef.finishTransaction(1); } catch(Exception e){ throw new RuntimeException(e); }
        }
    }
}