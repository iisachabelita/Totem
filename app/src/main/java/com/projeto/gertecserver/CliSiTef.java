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
    private final WebSocket conn;
    private int modalidade;
    private String valor;
    private String docFiscal;
    private String dataFiscal;
    private String horaFiscal;
    private String operador;
    private String restricoes;
    private int retry = 0;
    private String cupom;

    private String taxaServico;

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
        restricoes = parameters.optString("restricoes");

        retry = 0;

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
                    switch(modalidade){
                        case 2: // Débito
                            clisitef.continueTransaction("1"); // À vista
                            return;
                        case 3: // Crédito
                            try {
                                JSONObject jsonResponse = new JSONObject();
                                jsonResponse.put("message",new String(input));
                                conn.send(jsonResponse.toString());
                            } catch(JSONException e){}
                            return;
                    }
                    // Modalidade de pagamento no formato xxnn.
                    // xx corresponde ao grupo da modalidade e nn ao subgrupo.

                    // Grupo:
                    // - 00: Cheque
                    //    - 01: Cartão de Débito
                    //  - 02: Cartão de Crédito
                    //  - 03: Cartão tipo Voucher
                    //  - 05: Cartão Fidelidade
                    // - 98: Dinheiro
                    //  - 99: Outro tipo de cartão
                    // Subgrupo:
                    // - 00: À vista
                    // - 01: Pré-datado
                    // - 02: Parcelado com financiamento pelo estabelecimento
                    //  - 03: Parcelado com financiamento pela administradora
                    //  - 04: À vista com juros
                    // - 05: Crediário
                    //  - 99: Outro tipo de pagamento
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
                case CMD_GET_FIELD: // 30
                    try {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("message",new String(input));
                        jsonResponse.put("minLength",minLength);
                        jsonResponse.put("maxLength",maxLength);
                        conn.send(jsonResponse.toString());
                    } catch(JSONException e){}
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
                        cupom = new String(input);
                        break;
                }
                break;
            case CMD_GET_FIELD_CURRENCY: // 34
                // Valor monetário
                switch(fieldId){
                    case 504:
                        clisitef.continueTransaction(taxaServico);
                        return;
                }
                break;
            case CMD_ABORT_REQUEST: // 23
                // tratativa para controlar por quanto tempo irá aguardar o contato com periférico
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

    public void continueTransaction(String returnTransaction, String CMD){
        if(CMD.equals("CMD_GET_FIELD")){
            clisitef.setBuffer(returnTransaction);
            clisitef.continueTransaction("");
            return;
        }

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
            // Impressora impressora = new Impressora(context);
            // impressora.imprimirComprovante(cupom);
        }
    }
}