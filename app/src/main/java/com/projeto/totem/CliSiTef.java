package com.projeto.totem;

import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_ABORT_REQUEST;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MENU_TITLE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CLEAR_MSG_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRMATION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_CONFIRM_GO_BACK;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_FIELD_CURRENCY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_GET_MENU_OPTION;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_MESSAGE_QRCODE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_PRESS_ANY_KEY;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_REMOVE_QRCODE_FIELD;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MENU_TITLE;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_RESULT_DATA;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_MSG_CUSTOMER;
import static br.com.softwareexpress.sitef.android.CliSiTef.CMD_SHOW_QRCODE_FIELD;

import br.com.softwareexpress.sitef.android.ICliSiTefListener;

import com.projeto.totem.enums.Transaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    public final br.com.softwareexpress.sitef.android.CliSiTef clisitef;
    private final Map<String, Integer> retryMap = new HashMap<>();
    private Boolean firstTransaction = true;

    private SharedPreferences prefs = null;

    public String CAMPO_COMPROVANTE_CLIENTE = null;
    public String CAMPO_COMPROVANTE_ESTAB = null;

    Boolean management = false;
    Boolean pendingOrder = false;

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
            // Verificando se há transações pendentes
            prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);

            try {
                int returnPendingTransactions = clisitef.getQttPendingTransactions(
                    prefs.getString("dataFiscal", ""),
                    prefs.getString("docFiscal", "")
                );

                if(returnPendingTransactions > 0){
                    // Caso tiver trnasações pendentes, irá ser aceita
                    clisitef.finishTransaction(this,
                            // Recusar a transação, ENUM (0)
                            // Aceitar a transação, ENUM (1)
                            1,
                            prefs.getString("docFiscal", ""),
                            prefs.getString("dataFiscal", ""),
                            prefs.getString("horaFiscal", ""),
                            prefs.getString("ParametrosAdicionais", ParametrosAdicionais));

                    pendingOrder = true;
                }
            } catch(Exception e){ throw new RuntimeException(e); }

            clisitef.submitPendingMessages();

            prefs.edit().putString("mensagemPadrao",parameters.optString("mensagemPadrao")).apply();
            prefs.edit().putString("ParametrosAdicionais",parameters.optString("ParametrosAdicionais")).apply();
        }
    }

    public void configurarPinpad(){
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
        String valor = parameters.optString("valor","0");
        String docFiscal = parameters.optString("docFiscal","");
        String dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
        String operador = parameters.optString("operador","");
        String restricoes = parameters.optString("restricoes","");

        prefs = context.getSharedPreferences("CliSiTef", Context.MODE_PRIVATE);

        prefs.edit().putInt("modalidade",modalidade).apply();
        prefs.edit().putString("valor",valor).apply();
        prefs.edit().putString("docFiscal",docFiscal).apply();
        prefs.edit().putString("dataFiscal",dataFiscal).apply();
        prefs.edit().putString("horaFiscal",horaFiscal).apply();
        prefs.edit().putString("operador",operador).apply();
        prefs.edit().putString("restricoes",restricoes).apply();

        retryMap.clear();
        management = modalidade == 110 ? true : false;
        CAMPO_COMPROVANTE_CLIENTE = "";
        CAMPO_COMPROVANTE_ESTAB = "";

        int status = clisitef.startTransaction(this, modalidade, valor, docFiscal, dataFiscal, horaFiscal, operador, restricoes);
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
        String bridge = management ? "managementGeckoBridge" : "geckoBridge";

        JSONObject jsonResponse = new JSONObject();

        try {
            switch(command){
                case CMD_RESULT_DATA: // 0
                    if(fieldId == Transaction.CAMPO_NSU.getValor()){
                        jsonResponse.put("command", "nsu");
                        jsonResponse.put("nsu",clisitef.getBuffer());
                        MainActivity.sendToJS(jsonResponse);
                    }

                    if(fieldId == Transaction.CAMPO_COMPROVANTE_CLIENTE.getValor()) CAMPO_COMPROVANTE_CLIENTE = clisitef.getBuffer();
                    if(fieldId == Transaction.CAMPO_COMPROVANTE_ESTAB.getValor()) CAMPO_COMPROVANTE_ESTAB = clisitef.getBuffer();

                    clisitef.continueTransaction("");
                    break;

                case CMD_SHOW_MSG_CASHIER: // 1
                case CMD_SHOW_MSG_CUSTOMER: // 2
                case CMD_SHOW_MSG_CASHIER_CUSTOMER: // 3
                case CMD_PRESS_ANY_KEY: // 22
                case CMD_MESSAGE_QRCODE: // 52
                    jsonResponse.put("command", bridge);
                    jsonResponse.put("message",clisitef.getBuffer());
                    MainActivity.sendToJS(jsonResponse);
                    clisitef.continueTransaction("");
                    break;

                case CMD_SHOW_MENU_TITLE: // 4
                    jsonResponse.put("command", bridge);
                    jsonResponse.put("title",clisitef.getBuffer());
                    MainActivity.sendToJS(jsonResponse);
                    clisitef.continueTransaction("");
                    break;

                case CMD_GET_FIELD: // 30
                case CMD_GET_FIELD_CURRENCY: // 34
                    // if(!management){
                        // Crédito parcelado
                        // clisitef.continueTransaction("2");
                        // break;
                    // }

                    if(fieldId == Transaction.CAMPO_ADM.getValor()) bridge = "administrator";

                    jsonResponse.put("command", bridge);
                    jsonResponse.put("message",clisitef.getBuffer());
                    MainActivity.sendToJS(jsonResponse);
                    break;

                case CMD_CLEAR_MSG_CASHIER: // 11
                case CMD_CLEAR_MSG_CUSTOMER: // 12
                case CMD_CLEAR_MSG_CASHIER_CUSTOMER: // 13
                    jsonResponse.put("command", bridge);
                    jsonResponse.put("message","");
                    MainActivity.sendToJS(jsonResponse);
                    clisitef.continueTransaction("");
                    break;

                case CMD_CLEAR_MENU_TITLE: // 14
                    jsonResponse.put("command", bridge);
                    jsonResponse.put("title","");
                    MainActivity.sendToJS(jsonResponse);
                    clisitef.continueTransaction("");
                    break;

                case CMD_CONFIRM_GO_BACK: // 19
                case CMD_CONFIRMATION:// 20
                    // Confirma/cancela automaticamente
                    String inputMsg = clisitef.getBuffer();
                    String confirm;

                    int count = retryMap.getOrDefault(inputMsg, 0);
                    if(count < 3){
                        confirm = "0"; // Confirma
                        retryMap.put(inputMsg, count + 1);

                        if(!management){
                            jsonResponse.put("command", bridge);
                            jsonResponse.put("message", inputMsg);
                            MainActivity.sendToJS(jsonResponse);
                        }
                    } else{
                        confirm = "1"; // Cancela
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> clisitef.continueTransaction(confirm),5000 );
                    break;

                case CMD_GET_MENU_OPTION: // 21
                    jsonResponse.put("command", bridge);
                    jsonResponse.put("message", clisitef.getBuffer());
                    MainActivity.sendToJS(jsonResponse);
                    break;

                case CMD_ABORT_REQUEST: // 23 - esperando ação do usuário
                case CMD_SHOW_QRCODE_FIELD: // 50
                case CMD_REMOVE_QRCODE_FIELD: // 51
                default:
                    clisitef.continueTransaction("");
                    break;
            }
        } catch (JSONException e) { e.printStackTrace(); }

        if(new String(input).equals("13 - Operação Cancelada")){
            clisitef.abortTransaction(-1);
        }
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        JSONObject jsonResponse = new JSONObject();
        try { jsonResponse.put("command", "onTransactionResult");
        } catch(JSONException e){ e.printStackTrace(); }

        if(stage == 1 && resultCode == 0){
            if(management){
                // Reimpressão do cupomFiscal
                try {
                    MainActivity.impressora.imprimirComprovanteTransacao();
                    clisitef.finishTransaction(1);
                } catch(Exception e){ e.printStackTrace(); }
            } else{
                try {
                    // Impressão
                    jsonResponse.put("status", "print");
                    MainActivity.sendToJS(jsonResponse);
                } catch(Exception e){ e.printStackTrace(); }
            }
        } else if(stage == 2 && resultCode == 0){
            if(pendingOrder){
                // Envio da confirmação do pedido através de pending transactions
                try {
                    jsonResponse.put("status", "pendingOrder");
                    jsonResponse.put("value", prefs.getString("docFiscal", ""));
                } catch(JSONException e){ e.printStackTrace(); }
                MainActivity.sendToJS(jsonResponse);
                pendingOrder = false;
            } else if(!management){
                // Confirmação
                try {
                    jsonResponse.put("status", "success");
                } catch(JSONException e){ e.printStackTrace(); }
                MainActivity.sendToJS(jsonResponse);
            }

            if(firstTransaction){ configurarPinpad(); }
        } else{
            if(!management){
                String erro = "";
                switch(resultCode){
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
                        } else {
                            erro = "Não foi possível concluir o pagamento. Tente novamente.";
                        }
                        break;
                }

                try {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("value", erro);
                } catch(JSONException e){ e.printStackTrace(); }
                MainActivity.sendToJS(jsonResponse);
                if(firstTransaction){ configurarPinpad(); }
            }
        }
    }
}