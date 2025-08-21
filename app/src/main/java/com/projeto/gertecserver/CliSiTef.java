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
    private int retry = 0;

    public boolean configureCliSiTef = false;
    public boolean finishTransaction;

    public CliSiTef(Context context,WebSocket conn){
        this.context = context.getApplicationContext();
        this.clisitef = new br.com.softwareexpress.sitef.android.CliSiTef(this.context);
        this.conn = conn;
    }
    public void setWebSocket(WebSocket newConn) {
        this.conn = newConn;
    }
    public void configurarCliSiTef(JSONObject parameters){
        String IPSiTef = parameters.optString("IPSiTef");
        String IdLoja = parameters.optString("IdLoja");
        String IdTerminal = parameters.optString("IdTerminal");
        // String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;TipoComunicacaoExterna=SSL;“CaminhoCertificadoCA=ca_cert.pem”;]";
        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;]";

        int config = clisitef.configure(IPSiTef,IdLoja,IdTerminal,ParametrosAdicionais);

        if(config == 0){
            Log.e("CliSiTef", "CliSiTef configurado com sucesso");
            clisitef.pinpad.setDisplayMessage(parameters.optString("mensagemPadrao"));
            configureCliSiTef = true;
        } else{
            Log.e("CliSiTef", "Falha ao configurar CliSiTef. Código: " + config);
        }
    }

    public void transaction(JSONObject parameters){
        modalidade = parameters.optInt("modalidade");
        valor = parameters.optString("valor");
        docFiscal = parameters.optString("docFiscal");
        dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
        horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
        operador = parameters.optString("operador");
        restricoes = parameters.optString("restricoes","");

        retry = 0;
        finishTransaction = false;

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

        if(new String(input).equals("13 - Operação Cancelada")){
            clisitef.abortTransaction(-1);
        }

        if(stage == 1){
            switch(command){
                case CMD_GET_MENU_OPTION: // 21
                    // Débito e Crédito à vista
                    clisitef.continueTransaction("1");
                    return;
                case CMD_CONFIRMATION:// 20
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
                        clisitef.continueTransaction("0");
                        return;
                }
                break;
            case CMD_ABORT_REQUEST: // 23
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
                finishTransaction = true;
            } catch(Exception e){ throw new RuntimeException(e); }
        }

        if(stage == 2 && resultCode == 0){
             if(finishTransaction){
                 try {
                     // Impressão
                     JSONObject jsonResponse = new JSONObject();
                     jsonResponse.put("command", "submitOrder");
                     conn.send(jsonResponse.toString());
                 } catch(Exception e){ throw new RuntimeException(e); }
             }
        }

        if(resultCode != 0){
            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("message","erro: " + resultCode + " " + clisitef.getBuffer());
                conn.send(jsonResponse.toString());
            } catch(JSONException e){}
        }
    }
}