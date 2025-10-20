package com.projeto.totemserver;

import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRMATION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_CURRENCY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_MENU_OPTION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_PRESS_ANY_KEY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import br.com.softwareexpress.sitef.android.ICliSiTefListener;

public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    public final br.com.softwareexpress.sitef.android.CliSiTef clisitef;
    public boolean finishTransaction;
    private Boolean firstTransaction = true;
    private boolean inTransaction = false;

    public CliSiTef(Context context){
        this.context = context.getApplicationContext();
        this.clisitef = new br.com.softwareexpress.sitef.android.CliSiTef(this.context);
    }
    public void configurarCliSiTef(JSONObject parameters) throws Exception {
        String IPSiTef = parameters.optString("IPSiTef");
        String IdLoja = parameters.optString("IdLoja");
        String IdTerminal = parameters.optString("IdTerminal");
        String ParametrosAdicionais = parameters.optString("ParametrosAdicionais");

        int config = clisitef.configure(IPSiTef,IdLoja,IdTerminal,ParametrosAdicionais);

        if(config == 0){
            Log.d("CliSiTef", "CliSiTef configurado com sucesso");

            // Verificando se há transações pendentes
            SharedPreferences prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);

            try {
                int returnPendingTransactions = clisitef.getQttPendingTransactions(
                    prefs.getString("dataFiscal", ""),
                    prefs.getString("docFiscal", "")
                );

                if(returnPendingTransactions > 0){
                    // Caso tiver trnasações pendentes, irá ser cancelada
                    clisitef.finishTransaction(this,
                            // Recusar a transação, ENUM (0)
                            // Aceitar a transação, ENUM (1)
                            1,
                            prefs.getString("docFiscal", ""),
                            prefs.getString("dataFiscal", ""),
                            prefs.getString("horaFiscal", ""),
                            prefs.getString("ParametrosAdicionais", ParametrosAdicionais));
                }
            } catch(Exception e){ throw new RuntimeException(e); }

             clisitef.submitPendingMessages();

            // Trace
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
                @Override
                public void run(){
                    if(!inTransaction){
                        int trace = clisitef.startTransaction(CliSiTef.this, 121, prefs.getString("valor", ""), prefs.getString("docFiscal", ""), prefs.getString("dataFiscal", ""), prefs.getString("horaFiscal", ""), prefs.getString("operador", ""), prefs.getString("restricoes", ""));
                        Log.d("CliSiTef", "TRACE automático executado: " + trace);
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(this, 60000);
                }
            }, 60000);


            prefs.edit().putString("mensagemPadrao",parameters.optString("mensagemPadrao")).apply();
            prefs.edit().putString("ParametrosAdicionais",parameters.optString("ParametrosAdicionais")).apply();
        } else{ Log.e("CliSiTef", "Falha ao configurar CliSiTef. Código: " + config); }
    }

    public void configurarPinpad(){
        // SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);
        String mensagem = prefs.getString("mensagemPadrao","");

        try {
            if(clisitef.pinpad.isPresent()){
                clisitef.pinpad.setDisplayMessage(mensagem);
            }
            firstTransaction = false;
        } catch (Exception e){ throw new RuntimeException(e); }
    }

    public void transaction(JSONObject parameters) throws Exception {
        int modalidade = parameters.optInt("modalidade");
        String valor = parameters.optString("valor");
        String docFiscal = parameters.optString("docFiscal","");
        String dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
        String operador = parameters.optString("operador","");
        String restricoes = parameters.optString("restricoes","");

        SharedPreferences prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);

        prefs.edit().putInt("modalidade",modalidade).apply();
        prefs.edit().putString("valor",valor).apply();
        prefs.edit().putString("docFiscal",docFiscal).apply();
        prefs.edit().putString("dataFiscal",dataFiscal).apply();
        prefs.edit().putString("horaFiscal",horaFiscal).apply();
        prefs.edit().putString("operador",operador).apply();
        prefs.edit().putString("restricoes",restricoes).apply();

        finishTransaction = false;
        inTransaction = true;
        int status = clisitef.startTransaction(this, modalidade, valor, docFiscal, dataFiscal, horaFiscal, operador, restricoes);
        Log.d("CliSiTef", "START TRANSACTION: " + status);
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
        Log.d("CliSiTef", "onData, stage: " + stage + " command: " + command + " fieldId: " + fieldId + " minLength: " + minLength + " maxLength: " + maxLength + " input: " + new String(input));

        if(new String(input).equals("13 - Operação Cancelada")){
            clisitef.abortTransaction(-1);
            // try { clisitef.finishTransaction(0); } catch (Exception e) { throw new RuntimeException(e); }
        }

        if(stage == 1){
            switch(command){
                case CMD_GET_MENU_OPTION: // 21
                    // Débito e Crédito à vista
                    clisitef.continueTransaction("1");
                    return;
                case CMD_CONFIRMATION:// 20
                    try {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("message",new String(input));
                        MainActivity.sendToJS(jsonResponse);
                    } catch(JSONException e){}

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            // "1" // Cancela
                            clisitef.continueTransaction("0"),5000 // 5 segundos
                    );
                    return;
            case CMD_PRESS_ANY_KEY: // 22
            case CMD_SHOW_MSG_CASHIER_CUSTOMER: // 3
                try {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message",new String(input));
                    MainActivity.sendToJS(jsonResponse);
                } catch(JSONException e){}
                break;
            case CMD_GET_FIELD_CURRENCY: // 34
                // Valor monetário
                switch(fieldId){
                    case 504: // Taxa de Serviço
                        clisitef.continueTransaction("0");
                        return;
                }
                break;
            case CMD_CLEAR_MSG_CASHIER_CUSTOMER: // 13
                // Limpa mensagem enviada pelo CMD_PRESS_ANY_KEY & CMD_SHOW_MSG_CASHIER_CUSTOMER
                try {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message","");
                    MainActivity.sendToJS(jsonResponse);
                } catch(JSONException e){}
            }
        }
        clisitef.continueTransaction("");
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        Log.d("CliSiTef","onTransactionResult, stage " + stage + " resultCode: " + resultCode);

        JSONObject jsonResponse = new JSONObject();
        try { jsonResponse.put("command", "onTransactionResult"); } catch(JSONException e) {}

        SharedPreferences prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);

        if(stage == 1 && resultCode == 0){
            try {
                clisitef.finishTransaction(1);
                finishTransaction = true;
            } catch(Exception e){ }
        }

        if(resultCode != 0 || stage == 2 ){
            String erro = "";
            switch(resultCode){
                case 0:
                    erro = "Pagamento aprovado!";
                    break;
                case -2:
                case -6:
                case -15:
                    erro = "Pagamento cancelado.";
                    break;
                case -5:
                    erro = "Sem comunicação.";
                    break;
                case -40:
                case -41:
                    erro = "Pagamento recusado. Tente outro cartão.";
                    break;
                default:
                    if(resultCode > 0){
                        erro = "Pagamento recusado pelo banco.";
                    } else{
                        erro = "Não foi possível concluir o pagamento. Tente novamente.";
                    }
                    break;
            }

            if(finishTransaction){
                // Impressão
                try { jsonResponse.put("status", "success"); } catch(Exception e){}
                MainActivity.sendToJS(jsonResponse);
            } else if(resultCode == 0){
                try {
                    jsonResponse.put("status","pendingOrder");
                    jsonResponse.put("orderId",prefs.getString("docFiscal", ""));
                } catch(JSONException e){ e.printStackTrace(); }
                MainActivity.sendToJS(jsonResponse);
            } else if(resultCode != -100){
                try {
                    jsonResponse.put("status","error");
                    jsonResponse.put("erro",erro);
                } catch(JSONException e){ e.printStackTrace(); }
                MainActivity.sendToJS(jsonResponse);
            }

            if(firstTransaction){ configurarPinpad(); }
            inTransaction = false;
        }
    }
}