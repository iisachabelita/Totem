package com.projeto.gertecserver;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import br.com.softwareexpress.sitef.android.ICliSiTefListener;
public class CliSiTef implements ICliSiTefListener{
    private final Context context;
    private final br.com.softwareexpress.sitef.android.CliSiTef cliSiTef;
    private boolean isConfigured = false;

    public CliSiTef(Context context){
        this.context = context.getApplicationContext();
        this.cliSiTef = new br.com.softwareexpress.sitef.android.CliSiTef(this.context);
    }

    public String configurarCliSiTef(JSONObject json){
        String IPSiTef = json.optString("IPSiTef", "192.168.1.53");
        String IdLoja = json.optString("IdLoja", "00000000");
        String IdTerminal = json.optString("IdTerminal", "SE000001");
//        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;TipoComunicacaoExterna=SSL;CaminhoCertificadoCA=ca_cert.pem;]";
        String ParametrosAdicionais = "[TipoPinPad=ANDROID_USB;]";

        int config =  cliSiTef.configure(IPSiTef,IdLoja,IdTerminal,ParametrosAdicionais);

        if(config == 0){
            this.isConfigured = true;
            Log.i("CliSiTef", "CliSiTef configurado com sucesso");
            return "{\"status\":\"ok\",\"mensagem\":\"CliSiTef configurado com sucesso.\"}";
        } else{
            Log.e("CliSiTef", "Falha ao configurar CliSiTef. Código: " + config);
            return "{\"status\":\"erro\",\"mensagem\":\"Falha ao configurar CliSiTef. Código: " + config + "\"}";
        }
    }

    public String transaction(JSONObject json){
        try{
            if(!isConfigured){
                String config = configurarCliSiTef(json);
                JSONObject configStatus = new JSONObject(config);
                if(!"ok".equals(configStatus.optString("status"))){
                    return config;
                }
            }

            JSONObject parameters = json.optJSONObject("parameters");
            int modalidade = parameters != null ? parameters.optInt("modalidade", 0) : 0;
            String valor = parameters != null ? parameters.optString("valor", "1,00") : "1,00";
            String docFiscal = parameters != null ? parameters.optString("docFiscal", "01012025") : "01012025";
            String dataFiscal = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String horaFiscal = new SimpleDateFormat("HHmmss").format(new Date());
            String operador = parameters != null ? parameters.optString("operador", "") : "";
            String restricoes = parameters != null ? parameters.optString("restricoes", "") : "";

            int status = cliSiTef.startTransaction(this,modalidade,valor,docFiscal,dataFiscal,horaFiscal,operador,restricoes);

            if(status == 0){
                return "{\"status\":\"ok\",\"mensagem\":\"Transação iniciada.\"}";
            } else{
                return "{\"status\":\"erro\",\"mensagem\":\"Falha ao iniciar transação\",\"código\":" + status + "}";
            }
        } catch(Exception e){
            Log.e("CliSiTef", "Erro ao iniciar CliSiTef",e);
            return "{\"status\":\"erro\",\"mensagem\":\"Erro ao iniciar transação: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public void onData(int stage,int command,int fieldId,int minLength,int maxLength,byte[] input){
        if(stage == 1){
            // Evento onData recebido em uma startTransaction
        } else if(stage == 2){
            // Evento onData recebido em uma finishTransaction
        }

        Log.i("CliSiTef", "stage" + stage);
        Log.i("CliSiTef", "command" + command);
        Log.i("CliSiTef", "fieldId" + fieldId);
        Log.i("CliSiTef", "minLength" + minLength);
        Log.i("CliSiTef", "maxLength" + maxLength);
        Log.i("CliSiTef", "input" + Arrays.toString(input));;

//        switch(command){
//            case CliSiTef.CMD_RESULT_DATA:
//                switch(fieldId){
//                    case CAMPO_COMPROVANTE_CLIENTE: ///21
//                    case CAMPO_COMPROVANTE_ESTAB:   ///22
//                        // Tratar comando
//                        break;
//                }
//                break;
//
//            case CliSiTef.CMD_SHOW_MSG_CASHIER:
//            case CliSiTef.CMD_SHOW_MSG_CUSTOMER:
//            case CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER:
//                // Tratar comando
//                break;
//
//            case CliSiTef.CMD_SHOW_MENU_TITLE:
//            case CliSiTef.CMD_SHOW_HEADER:
//                // Tratar comando
//                break;
//
//            case CliSiTef.CMD_CLEAR_MSG_CASHIER:
//            case CliSiTef.CMD_CLEAR_MSG_CUSTOMER:
//            case CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER:
//            case CliSiTef.CMD_CLEAR_MENU_TITLE:
//            case CliSiTef.CMD_CLEAR_HEADER:
//                // Tratar comando
//                break;
//
//            case CliSiTef.CMD_CONFIRM_GO_BACK:
//            case CliSiTef.CMD_CONFIRMATION: {
//                // Tratar comando
//                return;
//            }
//
//            case CliSiTef.CMD_GET_FIELD_CURRENCY:
//            case CliSiTef.CMD_GET_FIELD_BARCODE:
//            case CliSiTef.CMD_GET_FIELD: {
//                // Tratar comando
//                return;
//            }
//
//            case CliSiTef.CMD_GET_MENU_OPTION: {
//                // Tratar comando
//                return;
//            }
//
//            case CliSiTef.CMD_PRESS_ANY_KEY: {
//                // Tratar comando
//                return;
//            }
//            case CliSiTef.CMD_ABORT_REQUEST:
//                break;
//
//            case CMD_SHOW_QRCODE_FIELD:
//                // Tratar comando
//                break;
//
//            case CliSiTef.CMD_REMOVE_QRCODE_FIELD:
//                // Tratar comando
//                break;
//
//            default:
//                break;
//        }

//        cliSiTef.continueTransaction(resposta);
        String resposta = "teste";
        cliSiTef.continueTransaction(resposta);
    }

    @Override
    public void onTransactionResult(int stage,int resultCode){
        if(stage == 1 && resultCode == 0){ // Confirm the transaction
            try{
                cliSiTef.finishTransaction(1);
            } catch (Exception e){
                Log.e("CliSiTef", "Erro ao finalizar transação",e);
            }
        } else{
            if(resultCode == 0){
                // Transação ok e pode exibir comprovante
            } else{
                // Finaliza aplicação
            }
        }
    }
}