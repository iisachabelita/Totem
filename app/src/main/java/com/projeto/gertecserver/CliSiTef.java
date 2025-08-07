package com.projeto.gertecserver;

import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_ABORT_REQUEST;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRMATION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_CURRENCY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_MENU_OPTION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_PRESS_ANY_KEY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_RESULT_DATA;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.softwareexpress.sitef.android.ICliSiTefListener;

public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    private final br.com.softwareexpress.sitef.android.CliSiTef clisitef;
    private WebSocket conn;
    private int modalidade;
    private String valor;
    private String docFiscal;
    private String dataFiscal;
    private String horaFiscal;
    private String operador;
    private String restricoes;
    private String credito;
    private int parcelas;
    private int retry = 0;
    private String taxaServico;
    // private long abortStartTime = 0L;
    // private boolean abortHandlingActive;

    public CliSiTef(Context context,WebSocket conn){
        this.context = context.getApplicationContext();
        this.clisitef = new br.com.softwareexpress.sitef.android.CliSiTef(this.context);
        this.conn = conn;
    }
    public void setWebSocket(WebSocket newConn) {
        this.conn = newConn;
    }
    public int configurarCliSiTef(JSONObject json){
        String IPSiTef = json.optString("IPSiTef");
        String IdLoja = json.optString("IdLoja");
        String IdTerminal = json.optString("IdTerminal");
//        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;TipoComunicacaoExterna=SSL;]";
        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;]";

        int config = clisitef.configure(IPSiTef,IdLoja,IdTerminal,ParametrosAdicionais);

        if(config == 0){
            MyWebSocketServer.isConfigured = true;
            Log.e("CliSiTef", "CliSiTef configurado com sucesso");
        } else{
            Log.e("CliSiTef", "Falha ao configurar CliSiTef. Código: " + config);
        }

        return config;
    }

    public void configurarEstabelecimento(JSONObject json){
        taxaServico = json.optString("taxaServico");
    }

    public void transaction(JSONObject json){
        if(!MyWebSocketServer.isConfigured){
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

        if(json.has("configurarEstabelecimento")){
            JSONObject config = json.optJSONObject("configurarEstabelecimento");
            configurarEstabelecimento(config);
        }

        JSONObject parameters = json.optJSONObject("parameters");
        modalidade = parameters.optInt("modalidade");
        valor = parameters.optString("valor");
        docFiscal = parameters.optString("docFiscal");
        dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
        horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
        operador = parameters.optString("operador");
        restricoes = parameters.optString("restricoes","");

        // Tratativa para modalidade de crédito parcelado
        credito = parameters.optString( "credito");
        parcelas = parameters.optInt("parcelas");

        retry = 0;
        // abortHandlingActive = false;

        int pendingMessages = clisitef.submitPendingMessages();

        if(pendingMessages == 0){
            int status = clisitef.startTransaction(this, modalidade, valor, docFiscal, dataFiscal, horaFiscal, operador, restricoes);
            Log.e("CliSiTef", "START TRANSACTION: " + status);
        }
    }

    @Override
    public void onData(
        int stage, // 1 - evento recebido em uma startTransaction; 2 - evento recebido em uma finishTransaction
        int command,
        int fieldId,
        int minLength,
        int maxLength,
        byte[] input
    ){
        Log.e("CliSiTef", "onData, stage: " + stage + " command: " + command + " fieldId: " + fieldId + " minLength: " + minLength + " maxLength: " + maxLength + " input: " + new String(input));

        if(clisitef.getBuffer().equals("13 - Operacao Cancelada?")){
            command = 53;
        }

        if(stage == 1){
            switch(command){
                case CMD_GET_MENU_OPTION: // 21
                    // if(command != CMD_ABORT_REQUEST && abortHandlingActive){ abortHandlingActive = false; }

                    switch(modalidade){
                        case 2: // Débito
                            clisitef.continueTransaction("1"); // À vista
                            return;
                        case 3: // Crédito
                            // À vista
                            if(parcelas == 1){
                                clisitef.continueTransaction("1");
                            } else{
                                // Parcelado
                                // 2 - pelo estabelecimento
                                // 3 - pela administradora
                                clisitef.continueTransaction(credito);
                            }
                            return;
                    }
                case CMD_CONFIRMATION:// 20
                    // if(command != CMD_ABORT_REQUEST && abortHandlingActive){ abortHandlingActive = false; }

                    // Erro leitura do cartão
                    String confirm;

                    if(retry < 3){
                        confirm = "0"; // Confirma
                        retry++;

                        try {
                            JSONObject jsonResponse = new JSONObject();
                            jsonResponse.put("message","Não foi possível ler o cartão. Tente novamente usando outra forma: insira, aproxime ou passe na tarja.");
                            conn.send(jsonResponse.toString());
                        } catch(JSONException e){}
                    } else{
                        confirm = "1"; // Cancela
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                        clisitef.continueTransaction(confirm),5000 // 5 segundos
                    );
                    return;
                case CMD_GET_FIELD: // 30
                    // if(command != CMD_ABORT_REQUEST && abortHandlingActive){ abortHandlingActive = false; }

                    switch(fieldId){
                        case 505: // Número de parcelas (DEVE SER > 1)
                            // try {
                                // JSONObject jsonResponse = new JSONObject();
                                // jsonResponse.put("message",new String(input));
                                // // jsonResponse.put("minLength",minLength); // está ignorando
                                // // jsonResponse.put("maxLength",maxLength); // está ignorando
                                // conn.send(jsonResponse.toString());
                            // } catch(JSONException e){}

                            continueTransaction(String.valueOf(parcelas));
                            break;
                    }
                     return;
            case CMD_PRESS_ANY_KEY: // 22
            case CMD_SHOW_MSG_CASHIER_CUSTOMER: // 3
                try {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message",new String(input));
                    conn.send(jsonResponse.toString());
                } catch(JSONException e){}
                break;
            case CMD_RESULT_DATA: // 0
                switch(fieldId){
                    case 121:
                        MyWebSocketServer.cupom = new String(input);
                        break;
                }
                break;
            case CMD_GET_FIELD_CURRENCY: // 34
                // Valor monetário
                switch(fieldId){
                    case 504:
                        // if(command != CMD_ABORT_REQUEST && abortHandlingActive){ abortHandlingActive = false; }

                        clisitef.continueTransaction(taxaServico);
                        return;
                }
                break;
            case CMD_ABORT_REQUEST: // 23
                // long now = System.currentTimeMillis();

                // if(!abortHandlingActive){
                    // abortStartTime = now;
                    // abortHandlingActive = true;
                // } else{
                    // long elapsed = now - abortStartTime;
                    // if(elapsed >= 40000) { // 40 segundos
                        // try { clisitef.finishTransaction(0); } catch(Exception e){} // Cancela
                        // abortHandlingActive = false;
                    // }
                // }
                break;
            case CMD_CLEAR_MSG_CASHIER_CUSTOMER: // 13
                // Limpa mensagem enviada pelo CMD_PRESS_ANY_KEY & CMD_SHOW_MSG_CASHIER_CUSTOMER
                try {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message","");
                    conn.send(jsonResponse.toString());
                } catch(JSONException e){}
            }
        }

        // if(command != CMD_ABORT_REQUEST && abortHandlingActive){ abortHandlingActive = false; }
        clisitef.continueTransaction("");
    }

    public void continueTransaction(String returnTransaction){
        clisitef.continueTransaction(returnTransaction);
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        Log.d("CliSiTef","onTransactionResult, stage " + stage + " resultCode: " + resultCode);

        if(stage == 1 && resultCode == 0){
            try {
                clisitef.finishTransaction(1);
            } catch(Exception e){
                throw new RuntimeException("Erro ao finalizar transação: " + e.getMessage());
            }
        }

        if(stage == 2 && resultCode == 0){
            // Impressão

            // JSONObject jsonResponse = new JSONObject();
            // try {
                // jsonResponse.put("command", "getOrderInfo");
                // conn.send(jsonResponse.toString());
            // } catch(JSONException e){}
        }

        if(resultCode != 0){
            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("status", "erro");
                jsonResponse.put("codigo",resultCode);
                jsonResponse.put("mensagem",clisitef.getBuffer());
                conn.send(jsonResponse.toString());
            } catch(JSONException e){}
        }
    }
}